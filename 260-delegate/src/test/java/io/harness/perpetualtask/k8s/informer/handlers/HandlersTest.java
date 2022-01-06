/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HandlersTest extends CategoryTest {
  private ClusterDetails clusterDetails = ClusterDetails.builder()
                                              .clusterId("cluster-id")
                                              .cloudProviderId("cloud-provider-id")
                                              .clusterName("cluster-name")
                                              .kubeSystemUid("cluster-uid")
                                              .build();
  private EventPublisher eventPublisher = mock(EventPublisher.class);

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCronJobHandler() throws Exception {
    assertThat(new V1beta1CronJobHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("CronJob");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testDaemonSetHandler() throws Exception {
    assertThat(new V1DaemonSetHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("DaemonSet");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testDeploymentHandler() throws Exception {
    assertThat(new V1DeploymentHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("Deployment");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testEventHandler() throws Exception {
    assertThat(new V1EventHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("Event");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testJobHandler() throws Exception {
    assertThat(new V1JobHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("Job");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNodeHandler() throws Exception {
    assertThat(new V1NodeHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("Node");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPodHandler() throws Exception {
    assertThat(new V1PodHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("Pod");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testReplicaSetHandler() throws Exception {
    assertThat(new V1ReplicaSetHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("ReplicaSet");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testStatefulSet() throws Exception {
    assertThat(new V1StatefulSetHandler(eventPublisher, clusterDetails).getKind()).isEqualTo("StatefulSet");
  }
}
