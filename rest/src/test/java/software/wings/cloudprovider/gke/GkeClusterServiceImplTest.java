package software.wings.cloudprovider.gke;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.Operation;
import com.google.api.services.container.model.UpdateClusterRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by brett on 2/10/17.
 */
public class GkeClusterServiceImplTest extends WingsBaseTest {
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private Container container;
  @Mock private Container.Projects projects;
  @Mock private Container.Projects.Zones zones;
  @Mock private Container.Projects.Zones.Clusters clusters;
  @Mock private Container.Projects.Zones.Clusters.Get clustersGet;
  @Mock private Container.Projects.Zones.Clusters.List clustersList;
  @Mock private Container.Projects.Zones.Clusters.Create clustersCreate;
  @Mock private Container.Projects.Zones.Clusters.Update clustersUpdate;
  @Mock private Container.Projects.Zones.Clusters.Delete clustersDelete;
  @Mock private Container.Projects.Zones.Operations operations;
  @Mock private Container.Projects.Zones.Operations.Get operationsGet;
  @Mock private HttpHeaders httpHeaders;

  @Inject @InjectMocks private GkeClusterService gkeClusterService;

  private static final ImmutableMap<String, String> PROJECT_PARAMS = ImmutableMap.<String, String>builder()
                                                                         .put("projectId", "project-a")
                                                                         .put("appName", "app-a")
                                                                         .put("zone", "zone-a")
                                                                         .build();
  private static final ImmutableMap<String, String> CLUSTER_PARAMS =
      ImmutableMap.<String, String>builder().putAll(PROJECT_PARAMS).put("name", "foo-bar").build();
  private static final ImmutableMap<String, String> CREATE_CLUSTER_PARAMS = ImmutableMap.<String, String>builder()
                                                                                .putAll(CLUSTER_PARAMS)
                                                                                .put("nodeCount", "1")
                                                                                .put("masterUser", "master")
                                                                                .put("masterPwd", "password")
                                                                                .build();

  private static final Cluster CLUSTER_1 =
      new Cluster()
          .setName("cluster-name-1")
          .setInitialNodeCount(5)
          .setStatus("RUNNING")
          .setEndpoint("1.1.1.1")
          .setMasterAuth((new MasterAuth().setUsername("master1").setPassword("password1")));
  private static final Cluster CLUSTER_2 =
      new Cluster()
          .setName("cluster-name-2")
          .setInitialNodeCount(5)
          .setStatus("RUNNING")
          .setEndpoint("1.1.1.2")
          .setMasterAuth((new MasterAuth().setUsername("master2").setPassword("password2")));

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getGkeContainerService(anyString())).thenReturn(container);
    when(container.projects()).thenReturn(projects);
    when(projects.zones()).thenReturn(zones);
    when(zones.clusters()).thenReturn(clusters);
    when(zones.operations()).thenReturn(operations);
    when(clusters.get(anyString(), anyString(), anyString())).thenReturn(clustersGet);
    when(clusters.list(anyString(), anyString())).thenReturn(clustersList);
    when(clusters.create(anyString(), anyString(), any(CreateClusterRequest.class))).thenReturn(clustersCreate);
    when(clusters.update(anyString(), anyString(), anyString(), any(UpdateClusterRequest.class)))
        .thenReturn(clustersUpdate);
    when(clusters.delete(anyString(), anyString(), anyString())).thenReturn(clustersDelete);
    when(operations.get(anyString(), anyString(), anyString())).thenReturn(operationsGet);
  }

  @Test
  public void shouldCreateCluster() throws Exception {
    IOException e =
        new GoogleJsonResponseException.Builder(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "not found", httpHeaders)
            .build();
    final List<Boolean> firstTime = new ArrayList<>(1);
    when(clustersGet.execute()).thenAnswer(invocation -> {
      if (firstTime.isEmpty()) {
        firstTime.add(true);
        throw e;
      }
      return CLUSTER_1;
    });
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersCreate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    KubernetesConfig config = gkeClusterService.createCluster(CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), anyString(), any(CreateClusterRequest.class));
    assertThat(config.getApiServerUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1");
  }

  @Test
  public void shouldNotCreateClusterIfExists() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);
    KubernetesConfig config = gkeClusterService.createCluster(CREATE_CLUSTER_PARAMS);

    verify(clusters, times(0)).create(anyString(), anyString(), any(CreateClusterRequest.class));
    assertThat(config.getApiServerUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1");
  }

  @Test
  public void shouldDeleteCluster() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersDelete.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.deleteCluster(CLUSTER_PARAMS);

    verify(clusters).delete(anyString(), anyString(), anyString());
    assertThat(success).isTrue();
  }

  @Test
  public void shouldNotDeleteClusterIfNotExists() throws Exception {
    when(clustersDelete.execute())
        .thenThrow(
            new GoogleJsonResponseException.Builder(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "not found", httpHeaders)
                .build());

    boolean success = gkeClusterService.deleteCluster(CLUSTER_PARAMS);

    verify(clusters).delete(anyString(), anyString(), anyString());
    assertThat(success).isFalse();
  }

  @Test
  public void shouldGetCluster() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    KubernetesConfig config = gkeClusterService.getCluster(CLUSTER_PARAMS);

    verify(clusters).get(anyString(), anyString(), anyString());
    assertThat(config.getApiServerUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1");
  }

  @Test
  public void shouldNotGetClusterIfNotExists() throws Exception {
    when(clustersGet.execute())
        .thenThrow(
            new GoogleJsonResponseException.Builder(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "not found", httpHeaders)
                .build());

    KubernetesConfig config = gkeClusterService.getCluster(CLUSTER_PARAMS);

    verify(clusters).get(anyString(), anyString(), anyString());
    assertThat(config).isNull();
  }

  @Test
  public void shouldListClusters() throws Exception {
    List<Cluster> clusterList = ImmutableList.of(CLUSTER_1, CLUSTER_2);
    when(clustersList.execute()).thenReturn(new ListClustersResponse().setClusters(clusterList));

    List<String> result = gkeClusterService.listClusters(PROJECT_PARAMS);

    verify(clusters).list(anyString(), anyString());
    assertThat(result).containsExactlyInAnyOrder(CLUSTER_1.getName(), CLUSTER_2.getName());
  }

  @Test
  public void shouldNotListClustersIfError() throws Exception {
    when(clustersList.execute()).thenThrow(new IOException());

    List<String> result = gkeClusterService.listClusters(PROJECT_PARAMS);

    verify(clusters).list(anyString(), anyString());
    assertThat(result).isNull();
  }

  @Test
  public void shouldSetNodePoolAutoscaling() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersUpdate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.setNodePoolAutoscaling(true, 2, 4, CLUSTER_PARAMS);

    verify(clusters).update(anyString(), anyString(), anyString(), any(UpdateClusterRequest.class));
    assertThat(success).isTrue();
  }

  @Test
  public void shouldNotSetNodePoolAutoscalingIfError() throws Exception {
    when(clustersUpdate.execute()).thenThrow(new IOException());

    boolean success = gkeClusterService.setNodePoolAutoscaling(true, 2, 4, CLUSTER_PARAMS);

    verify(clusters).update(anyString(), anyString(), anyString(), any(UpdateClusterRequest.class));
    assertThat(success).isFalse();
  }
}
