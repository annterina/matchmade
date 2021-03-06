package algorithm;

import clients.ClientSearchingData;
import clients.ClientSelfData;
import clients.PoolClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import configuration.Configuration;
import configuration.ConfigurationParameters;
import matchmaker.ClientPool;
import net.sf.javaml.core.kdtree.KDTree;
import org.assertj.core.internal.Integers;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MatchSearchTreeTest
{
    private static final PoolClient DUMMY_POOL_CLIENT = getDummyPoolClient(1);
    private static final Set<PoolClient> DUMMY_POOL_CLIENT_SET = getDummyPoolClientSet(10);
    private Configuration configuration;
    private ClientPool clientPool;
    private ConfigurationParameters configurationParameters;
    private Map<Integer, Set<PoolClient>> clientMatches;
    private KDTree searchTree;

    private MatchSearchTree matchSearchTree;

    @Before
    public void setUp()
    {
        configuration = mock(Configuration.class);
        clientPool = mock(ClientPool.class);
        configurationParameters = mock(ConfigurationParameters.class);
        clientMatches = new HashMap<>();
        searchTree = mock(KDTree.class);
        given(configuration.getConfigurationParameters()).willReturn(configurationParameters);
        matchSearchTree = new MatchSearchTree(
                clientPool,
                configuration,
                clientMatches,
                searchTree
        );
    }

    @Test
    public void shouldPassIterationWithNoClients()
    {
        //given
        //when
        matchSearchTree.matchIteration();
        //then
        assertThat(clientMatches).isEmpty();
        verify(clientPool, times(2)).getClients();
        verify(clientPool).expandClientsParameters();
    }

    @Test
    public void shouldAddClientsToTree()
    {
        //given
        given(clientPool.getClients()).willReturn(DUMMY_POOL_CLIENT_SET);
        //when
        matchSearchTree.fillSearchTree();
        //then
        verify(searchTree, times(DUMMY_POOL_CLIENT_SET.size())).insert(any(), any());
    }

    @Test
    public void shouldClearClientMatches()
    {
        //given
        final Map<Integer, Set<PoolClient>> clientMatches = new HashMap<>();
        clientMatches.put(5, ImmutableSet.of());
        final MatchSearchTree matchSearchTree = new MatchSearchTree(
                clientPool,
                configuration,
                clientMatches,
                searchTree
        );
        //when
        matchSearchTree.clearSearchTree();
        //then
        assertThat(clientMatches).isEmpty();
    }

    @Test
    public void shouldFillClientsMatches() throws Exception
    {
        // given
        PoolClient poolClient1 = getDummyPoolClient(1);
        PoolClient poolClient2 = getDummyPoolClient(2);
        PoolClient poolClient3 = getDummyPoolClient(3);
        Map<Integer, Set<PoolClient>> clientMatches = new HashMap<>();
        given(clientPool.getClients()).willReturn(new HashSet<>(Arrays.asList(
            poolClient1,
            poolClient2,
            poolClient3
        )));
        given(searchTree.range(any(double[].class), any(double[].class))).willReturn(new Object[]{poolClient1});
        final MatchSearchTree matchSearchTree = new MatchSearchTree(
                clientPool,
                configuration,
                clientMatches,
                searchTree
        );

        // when
        matchSearchTree.fillClientsMatches();

        // then
        assertThat(clientMatches).hasSize(3);
    }

    @Test
    public void shouldNotFindMatchingSetForGivenClient() throws Exception
    {
        // given
        given(searchTree.range(any(double[].class), any(double[].class))).willReturn(new Object[]{DUMMY_POOL_CLIENT});

        // when
        Set<PoolClient> matchingSetForDummyClient = matchSearchTree.findMatchingSetFor(DUMMY_POOL_CLIENT);

        // then
        assertThat(matchingSetForDummyClient).hasSize(0);
    }

    @Test
    public void shouldFindMatchingSetForGivenClient() throws Exception
    {
        // given
        PoolClient poolClient = getDummyPoolClient(2);
        given(searchTree.range(any(double[].class), any(double[].class))).willReturn(new Object[]{
                DUMMY_POOL_CLIENT,
                poolClient});

        // when
        Set<PoolClient> matchingSetForDummyClient = matchSearchTree.findMatchingSetFor(DUMMY_POOL_CLIENT);

        // then
        assertThat(matchingSetForDummyClient).hasSize(1);
    }

    @Test
    public void shouldTryCreatingAMatchFromGivenClients() throws Exception
    {
        // given
        int teamSize = 3;
        Configuration configuration = mock(Configuration.class, withSettings().stubOnly());
        ConfigurationParameters configurationParameters = mock(ConfigurationParameters.class,
                                                               withSettings().stubOnly());
        given(configuration.getConfigurationParameters()).willReturn(configurationParameters);
        given(configurationParameters.getTeamSize()).willReturn(teamSize);
        PoolClient dummyPoolClient1 = getDummyPoolClient(1);
        PoolClient dummyPoolClient2 = getDummyPoolClient(2);
        PoolClient dummyPoolClient3 = getDummyPoolClient(3);
        Map<Integer, Set<PoolClient>> clientMatches = new HashMap<>();
        clientMatches.put(1, new HashSet<>(Arrays.asList(dummyPoolClient2, dummyPoolClient3)));
        clientMatches.put(2, new HashSet<>(Arrays.asList(dummyPoolClient1, dummyPoolClient3)));
        clientMatches.put(3, new HashSet<>(Arrays.asList(dummyPoolClient1, dummyPoolClient2)));

        final MatchSearchTree matchSearchTree = new MatchSearchTree(
                clientPool,
                configuration,
                clientMatches,
                searchTree
        );

        // when
        Set<PoolClient> foundMatch = matchSearchTree.tryCreatingAMatchFrom(
                dummyPoolClient1,
                ImmutableSet.of(dummyPoolClient2, dummyPoolClient3));

        // then
        assertThat(foundMatch).hasSize(3);
    }

    private static Set<PoolClient> getDummyPoolClientSet(int numberOfClients)
    {
        final Set<PoolClient> set = new HashSet<>();
        for (int i=0; i<numberOfClients; i++) {
            set.add(getDummyPoolClient(i));
        }
        return set;
    }

    private static PoolClient getDummyPoolClient(int clientId)
    {
        final ClientSelfData clientSelfData = mock(ClientSelfData.class, withSettings().stubOnly());
        final ClientSearchingData clientSearchingData = mock(ClientSearchingData.class, withSettings().stubOnly());
        final PoolClient poolClient = mock(PoolClient.class, withSettings().stubOnly());
        given(clientSelfData.getParameters()).willReturn(ImmutableMap.of());
        given(clientSearchingData.getParameters()).willReturn(ImmutableMap.of());
        given(poolClient.getPrioritizedSearchingData()).willReturn(clientSearchingData);
        given(poolClient.getSelfData()).willReturn(clientSelfData);
        given(poolClient.getClientID()).willReturn(clientId);
        return poolClient;
    }
}
