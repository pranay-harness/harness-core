package software.wings.cloudprovider.gke;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.Operation;
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

import static com.google.common.truth.Truth.assertThat;
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
  @Mock private Container.Projects.Zones.Clusters.Create clustersCreate;
  @Mock private Container.Projects.Zones.Operations operations;
  @Mock private Container.Projects.Zones.Operations.Get operationsGet;
  @Mock private HttpHeaders httpHeaders;

  @Inject @InjectMocks private GkeClusterService gkeClusterService;

  private static final ImmutableMap<String, String> CLUSTER_SPEC = ImmutableMap.<String, String>builder()
                                                                       .put("name", "foo-bar")
                                                                       .put("projectId", "kubernetes-test-158122")
                                                                       .put("appName", "testApp")
                                                                       .put("zone", "us-west1-a")
                                                                       .put("nodeCount", "1")
                                                                       .put("masterUser", "master")
                                                                       .put("masterPwd", "password")
                                                                       .build();

  private static final Cluster CLUSTER =
      new Cluster()
          .setName("clusterName")
          .setInitialNodeCount(5)
          .setStatus("RUNNING")
          .setEndpoint("1.2.3.4")
          .setMasterAuth((new MasterAuth().setUsername("master").setPassword("password")));

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getGkeContainerService(anyString())).thenReturn(container);
    when(container.projects()).thenReturn(projects);
    when(projects.zones()).thenReturn(zones);
    when(zones.clusters()).thenReturn(clusters);
    when(clusters.get(anyString(), anyString(), anyString())).thenReturn(clustersGet);
    when(clusters.create(anyString(), anyString(), any(CreateClusterRequest.class))).thenReturn(clustersCreate);
    when(zones.operations()).thenReturn(operations);
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
      return CLUSTER;
    });
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersCreate.execute()).thenReturn(pendingOperation);
    Operation doneOperation = new Operation().setStatus("DONE");
    when(operationsGet.execute()).thenReturn(doneOperation);

    KubernetesConfig config = gkeClusterService.createCluster(CLUSTER_SPEC);

    verify(clusters, times(1)).create(anyString(), anyString(), any(CreateClusterRequest.class));
    assertThat(config.getApiServerUrl()).isEqualTo("https://1.2.3.4/");
    assertThat(config.getUsername()).isEqualTo("master");
    assertThat(config.getPassword()).isEqualTo("password");
  }

  @Test
  public void shouldNotCreateClusterIfExists() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER);
    KubernetesConfig config = gkeClusterService.createCluster(CLUSTER_SPEC);

    verify(clusters, times(0)).create(anyString(), anyString(), any(CreateClusterRequest.class));
    assertThat(config.getApiServerUrl()).isEqualTo("https://1.2.3.4/");
    assertThat(config.getUsername()).isEqualTo("master");
    assertThat(config.getPassword()).isEqualTo("password");
  }
}
