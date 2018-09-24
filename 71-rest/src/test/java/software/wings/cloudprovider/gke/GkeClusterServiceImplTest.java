package software.wings.cloudprovider.gke;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.GcpHelperService.LOCATION_DELIMITER;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.NodePool;
import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.api.services.container.model.Operation;
import com.google.api.services.container.model.UpdateClusterRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by brett on 2/10/17.
 */
public class GkeClusterServiceImplTest extends WingsBaseTest {
  @Mock private GcpHelperService gcpHelperService;
  @Mock private Container container;
  @Mock private Container.Projects projects;
  @Mock private Container.Projects.Locations locations;
  @Mock private Container.Projects.Locations.Clusters clusters;
  @Mock private Container.Projects.Locations.Clusters.Get clustersGet;
  @Mock private Container.Projects.Locations.Clusters.List clustersList;
  @Mock private Container.Projects.Locations.Clusters.Create clustersCreate;
  @Mock private Container.Projects.Locations.Clusters.Update clustersUpdate;
  @Mock private Container.Projects.Locations.Clusters.Delete clustersDelete;
  @Mock private Container.Projects.Locations.Operations operations;
  @Mock private Container.Projects.Locations.Operations.Get operationsGet;
  @Mock private HttpHeaders httpHeaders;

  @Inject @InjectMocks private GkeClusterService gkeClusterService;
  private GoogleJsonResponseException notFoundException;

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withValue(
              GcpConfig.builder().serviceAccountKeyFileContent("{\"project_id\": \"project-a\"}".toCharArray()).build())
          .build();
  private static final String ZONE_CLUSTER = "zone-a/foo-bar";
  private static final ImmutableMap<String, String> CREATE_CLUSTER_PARAMS = ImmutableMap.<String, String>builder()
                                                                                .put("nodeCount", "1")
                                                                                .put("masterUser", "master")
                                                                                .put("masterPwd", "password")
                                                                                .build();

  private static final Cluster CLUSTER_1 =
      new Cluster()
          .setName("cluster-name-1")
          .setZone("zone-a")
          .setInitialNodeCount(5)
          .setStatus("RUNNING")
          .setEndpoint("1.1.1.1")
          .setMasterAuth(new MasterAuth().setUsername("master1").setPassword("password1"))
          .setNodePools(ImmutableList.of(
              new NodePool()
                  .setName("node-pool1.1")
                  .setAutoscaling(new NodePoolAutoscaling().setEnabled(false).setMinNodeCount(1).setMaxNodeCount(2)),
              new NodePool()
                  .setName("node-pool1.2")
                  .setAutoscaling(new NodePoolAutoscaling().setEnabled(true).setMinNodeCount(1).setMaxNodeCount(3))));

  private static final Cluster CLUSTER_2 =
      new Cluster()
          .setName("cluster-name-2")
          .setZone("zone-b")
          .setInitialNodeCount(5)
          .setStatus("RUNNING")
          .setEndpoint("1.1.1.2")
          .setMasterAuth(new MasterAuth().setUsername("master2").setPassword("password2"))
          .setNodePools(ImmutableList.of(
              new NodePool()
                  .setName("node-pool2")
                  .setAutoscaling(new NodePoolAutoscaling().setEnabled(true).setMinNodeCount(5).setMaxNodeCount(10))));

  @Before
  public void setUp() throws Exception {
    when(gcpHelperService.getGkeContainerService(any(), any())).thenReturn(container);
    when(gcpHelperService.getSleepIntervalSecs()).thenReturn(0);
    when(gcpHelperService.getTimeoutMins()).thenReturn(1);
    when(container.projects()).thenReturn(projects);
    when(projects.locations()).thenReturn(locations);
    when(locations.clusters()).thenReturn(clusters);
    when(clusters.get(anyString())).thenReturn(clustersGet);
    when(clusters.list(anyString())).thenReturn(clustersList);
    when(locations.operations()).thenReturn(operations);
    when(clusters.create(anyString(), any(CreateClusterRequest.class))).thenReturn(clustersCreate);
    when(clusters.update(anyString(), any(UpdateClusterRequest.class))).thenReturn(clustersUpdate);
    when(clusters.delete(anyString())).thenReturn(clustersDelete);
    when(operations.get(anyString())).thenReturn(operationsGet);

    GoogleJsonError googleJsonError = new GoogleJsonError();
    googleJsonError.setCode(HttpStatusCodes.STATUS_CODE_NOT_FOUND);
    notFoundException = new GoogleJsonResponseException(
        new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "not found", httpHeaders),
        googleJsonError);
    on(gkeClusterService).set("timeLimiter", new FakeTimeLimiter());
  }

  @Test
  public void shouldCreateCluster() throws Exception {
    final List<Boolean> firstTime = new ArrayList<>(1);
    when(clustersGet.execute()).thenAnswer(invocation -> {
      if (firstTime.isEmpty()) {
        firstTime.add(true);
        throw notFoundException;
      }
      return CLUSTER_1;
    });
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersCreate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    KubernetesConfig config = gkeClusterService.createCluster(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  public void shouldNotCreateClusterIfExists() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);
    KubernetesConfig config = gkeClusterService.createCluster(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters, times(0)).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  public void shouldNotCreateClusterIfError() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);
    when(clustersCreate.execute()).thenThrow(new IOException());

    KubernetesConfig config = gkeClusterService.createCluster(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config).isNull();
  }

  @Test
  public void shouldNotCreateClusterIfOperationQueryFailed() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersCreate.execute()).thenReturn(pendingOperation);
    when(operationsGet.execute()).thenThrow(new IOException());

    KubernetesConfig config = gkeClusterService.createCluster(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config).isNull();
  }

  @Test
  public void shouldDeleteCluster() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersDelete.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER);

    verify(clusters).delete(anyString());
    assertThat(success).isTrue();
  }

  @Test
  public void shouldNotDeleteClusterIfNotExists() throws Exception {
    when(clustersDelete.execute()).thenThrow(notFoundException);

    boolean success = gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER);

    verify(clusters).delete(anyString());
    assertThat(success).isFalse();
  }

  @Test
  public void shouldNotDeleteClusterIfOperationFailed() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersDelete.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("FAILED");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER);

    verify(clusters).delete(anyString());
    assertThat(success).isFalse();
  }

  @Test
  public void shouldNotDeleteClusterIfOperationQueryFailed() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersDelete.execute()).thenReturn(pendingOperation);
    when(operationsGet.execute()).thenThrow(new IOException());

    boolean success = gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER);

    verify(clusters).delete(anyString());
    assertThat(success).isFalse();
  }

  @Test
  public void shouldGetCluster() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    KubernetesConfig config =
        gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default");

    verify(clusters).get(anyString());
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1");
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  public void shouldNotGetClusterIfNotExists() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);

    try {
      gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  public void shouldNotGetClusterIfError() throws Exception {
    when(clustersGet.execute()).thenThrow(new IOException());

    try {
      gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  public void shouldNotGetClusterIfOtherJsonError() throws Exception {
    GoogleJsonError googleJsonError = new GoogleJsonError();
    googleJsonError.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
    when(clustersGet.execute())
        .thenThrow(new GoogleJsonResponseException(
            new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_FORBIDDEN, "forbidden", httpHeaders),
            googleJsonError));

    try {
      gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  public void shouldListClusters() throws Exception {
    List<Cluster> clusterList = ImmutableList.of(CLUSTER_1, CLUSTER_2);
    when(clustersList.execute()).thenReturn(new ListClustersResponse().setClusters(clusterList));

    List<String> result = gkeClusterService.listClusters(COMPUTE_PROVIDER_SETTING, Collections.emptyList());

    verify(clusters).list(anyString());
    assertThat(result).containsExactlyInAnyOrder(CLUSTER_1.getZone() + LOCATION_DELIMITER + CLUSTER_1.getName(),
        CLUSTER_2.getZone() + LOCATION_DELIMITER + CLUSTER_2.getName());
  }

  @Test
  public void shouldNotListClustersIfError() throws Exception {
    when(clustersList.execute()).thenThrow(new IOException());

    List<String> result = gkeClusterService.listClusters(COMPUTE_PROVIDER_SETTING, Collections.emptyList());

    verify(clusters).list(anyString());
    assertThat(result).isNull();
  }

  @Test
  public void shouldSetNodePoolAutoscaling() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersUpdate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.setNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null, true, 2, 4);

    verify(clusters).update(anyString(), any(UpdateClusterRequest.class));
    assertThat(success).isTrue();
  }

  @Test
  public void shouldSetNodePoolAutoscalingWithPoolId() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersUpdate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    boolean success = gkeClusterService.setNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "pool-id", true, 2, 4);

    ArgumentCaptor<UpdateClusterRequest> args = ArgumentCaptor.forClass(UpdateClusterRequest.class);
    verify(clusters).update(anyString(), args.capture());
    assertThat(success).isTrue();
    assertThat(args.getValue().getUpdate().getDesiredNodePoolId()).isEqualTo("pool-id");
  }

  @Test
  public void shouldNotSetNodePoolAutoscalingIfError() throws Exception {
    when(clustersUpdate.execute()).thenThrow(new IOException());

    boolean success = gkeClusterService.setNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null, true, 2, 4);

    verify(clusters).update(anyString(), any(UpdateClusterRequest.class));
    assertThat(success).isFalse();
  }

  @Test
  public void shouldNotSetNodePoolAutoscalingIfOperationQueryFailed() throws Exception {
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersUpdate.execute()).thenReturn(pendingOperation);
    when(operationsGet.execute()).thenThrow(new IOException());

    boolean success = gkeClusterService.setNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null, true, 2, 4);

    verify(clusters).update(anyString(), any(UpdateClusterRequest.class));
    assertThat(success).isFalse();
  }

  @Test
  public void shouldGetNodePoolAutoscaling() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_2);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null);

    verify(clusters).get(anyString());
    assertThat(autoscaling).isNotNull();
    assertThat(autoscaling.getEnabled()).isTrue();
    assertThat(autoscaling.getMinNodeCount()).isEqualTo(5);
    assertThat(autoscaling.getMaxNodeCount()).isEqualTo(10);
  }

  @Test
  public void shouldGetNodePoolAutoscalingWithPoolId() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    NodePoolAutoscaling autoscaling = gkeClusterService.getNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "node-pool1.2");

    verify(clusters).get(anyString());
    assertThat(autoscaling).isNotNull();
    assertThat(autoscaling.getEnabled()).isTrue();
    assertThat(autoscaling.getMinNodeCount()).isEqualTo(1);
    assertThat(autoscaling.getMaxNodeCount()).isEqualTo(3);
  }

  @Test
  public void shouldNotGetNodePoolAutoscalingIfNotExists() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null);

    verify(clusters).get(anyString());
    assertThat(autoscaling).isNull();
  }

  @Test
  public void shouldNotGetNodePoolAutoscalingIfNodePoolNotExists() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    NodePoolAutoscaling autoscaling = gkeClusterService.getNodePoolAutoscaling(
        COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "node-pool-missing");

    verify(clusters).get(anyString());
    assertThat(autoscaling).isNull();
  }

  @Test
  public void shouldFailOnMissingNodePoolIdWhenMultiplePools() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    try {
      gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
      assertThat(e.getMessage()).startsWith("expected one element but was:");
    }
  }
}
