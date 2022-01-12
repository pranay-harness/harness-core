/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.gcp.helpers;

import static io.harness.delegate.task.gcp.helpers.GcpHelperService.LOCATION_DELIMITER;
import static io.harness.rule.OwnerRule.ABHINAV2;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.GcpServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.rule.Owner;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class GkeClusterHelperTest extends CategoryTest {
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

  @Inject @InjectMocks private GkeClusterHelper gkeClusterHelper;
  private GoogleJsonResponseException notFoundException;

  private final char[] serviceAccountKey = "{\"project_id\": \"project-a\"}".toCharArray();
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
    MockitoAnnotations.initMocks(this);
    when(gcpHelperService.getGkeContainerService(serviceAccountKey, false)).thenReturn(container);
    when(gcpHelperService.getGkeContainerService(null, true)).thenReturn(container);
    when(gcpHelperService.getSleepIntervalSecs()).thenReturn(0);
    when(gcpHelperService.getTimeoutMins()).thenReturn(1);
    when(gcpHelperService.getClusterProjectId(any())).thenReturn("project-a");
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
    on(gkeClusterHelper).set("timeLimiter", new FakeTimeLimiter());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
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

    KubernetesConfig config =
        gkeClusterHelper.createCluster(serviceAccountKey, false, ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1".toCharArray());
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotCreateClusterIfExists() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);
    KubernetesConfig config =
        gkeClusterHelper.createCluster(serviceAccountKey, false, ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters, times(0)).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1".toCharArray());
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotCreateClusterIfError() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);
    when(clustersCreate.execute()).thenThrow(new IOException());

    KubernetesConfig config =
        gkeClusterHelper.createCluster(serviceAccountKey, false, ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotCreateClusterIfOperationQueryFailed() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);
    Operation pendingOperation = new Operation().setStatus("RUNNING");
    when(clustersCreate.execute()).thenReturn(pendingOperation);
    when(operationsGet.execute()).thenThrow(new IOException());

    KubernetesConfig config =
        gkeClusterHelper.createCluster(serviceAccountKey, false, ZONE_CLUSTER, "default", CREATE_CLUSTER_PARAMS);

    verify(clusters).create(anyString(), any(CreateClusterRequest.class));
    assertThat(config).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetCluster() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    KubernetesConfig config = gkeClusterHelper.getCluster(serviceAccountKey, false, ZONE_CLUSTER, "default");

    verify(clusters).get(anyString());
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1".toCharArray());
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetClusterWithInheritedCredentials() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);

    KubernetesConfig config = gkeClusterHelper.getCluster(null, true, ZONE_CLUSTER, "default");

    verify(clusters).get(anyString());
    assertThat(config.getMasterUrl()).isEqualTo("https://1.1.1.1/");
    assertThat(config.getUsername()).isEqualTo("master1".toCharArray());
    assertThat(config.getPassword()).isEqualTo("password1".toCharArray());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForInvalidClusterName() throws Exception {
    when(clustersGet.execute()).thenReturn(CLUSTER_1);
    assertThatThrownBy(() -> gkeClusterHelper.getCluster(null, true, null, "default"))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> gkeClusterHelper.getCluster(null, true, "foo", "default"))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetClusterIfNotExists() throws Exception {
    when(clustersGet.execute()).thenThrow(notFoundException);

    try {
      gkeClusterHelper.getCluster(serviceAccountKey, false, ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetClusterIfError() throws Exception {
    when(clustersGet.execute()).thenThrow(new IOException());

    try {
      gkeClusterHelper.getCluster(serviceAccountKey, false, ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotGetClusterIfOtherJsonError() throws Exception {
    GoogleJsonError googleJsonError = new GoogleJsonError();
    googleJsonError.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
    when(clustersGet.execute())
        .thenThrow(new GoogleJsonResponseException(
            new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_FORBIDDEN, "forbidden", httpHeaders),
            googleJsonError));

    try {
      gkeClusterHelper.getCluster(serviceAccountKey, false, ZONE_CLUSTER, "default");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Expected
    }

    verify(clusters).get(anyString());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldListClusters() throws Exception {
    List<Cluster> clusterList = ImmutableList.of(CLUSTER_1, CLUSTER_2);
    when(clustersList.execute()).thenReturn(new ListClustersResponse().setClusters(clusterList));

    List<String> result = gkeClusterHelper.listClusters(serviceAccountKey, false);

    verify(clusters).list(anyString());
    assertThat(result).containsExactlyInAnyOrder(CLUSTER_1.getZone() + LOCATION_DELIMITER + CLUSTER_1.getName(),
        CLUSTER_2.getZone() + LOCATION_DELIMITER + CLUSTER_2.getName());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotListClustersIfError() throws Exception {
    when(clustersList.execute()).thenThrow(new IOException());

    assertThatThrownBy(() -> gkeClusterHelper.listClusters(serviceAccountKey, false))
        .isInstanceOf(GcpServerException.class);

    verify(clusters).list(anyString());
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void shouldNotListClustersIfResponseException() throws IOException {
    GoogleJsonError googleJsonError = new GoogleJsonError();
    googleJsonError.setMessage("Simulated Google Json Error");

    GoogleJsonResponseException responseException = new GoogleJsonResponseException(
        new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, "Forbidden", httpHeaders),
        googleJsonError);
    when(clustersList.execute()).thenThrow(responseException);
    assertThatThrownBy(() -> gkeClusterHelper.listClusters(serviceAccountKey, false))
        .isInstanceOf(GcpServerException.class);
  }
}
