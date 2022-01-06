/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.service.impl;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.service.intfc.InstanceInfoTimescaleDAO;
import io.harness.category.element.UnitTests;
import io.harness.event.payloads.Lifecycle;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class InstanceInfoTimescaleDAOImplTest extends CategoryTest {
  private InstanceInfoTimescaleDAO instanceInfoTimescaleDAO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    final MockDataProvider provider = mock(MockDataProvider.class);
    final MockConnection connection = new MockConnection(provider);
    final DSLContext dslContext = DSL.using(connection, SQLDialect.POSTGRES);

    instanceInfoTimescaleDAO = new InstanceInfoTimescaleDAOImpl(dslContext);

    final MockResult[] mockResults = {new MockResult(1)};
    when(provider.execute(any())).thenReturn(mockResults);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testInsertIntoNodeInfo() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.insertIntoNodeInfo(createInstanceInfo()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testInsertIntoNodeInfoList() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.insertIntoNodeInfo(ImmutableList.of(createInstanceInfo())));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testInsertIntoWorkloadInfo() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.insertIntoWorkloadInfo("", createK8sWorkloadSpec()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testInsertIntoPodInfoList() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.insertIntoPodInfo(ImmutableList.of(createInstanceInfo())));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testInsertIntoPodInfo() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.insertIntoPodInfo(createInstanceInfo()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdatePodStopEvent() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.updatePodStopEvent(ImmutableList.of(createInstanceEvent())));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdatePodStopLifecycleEvent() {
    assertDoesNotThrow(()
                           -> instanceInfoTimescaleDAO.updatePodLifecycleEvent(
                               "", ImmutableList.of(createLifecycleEvent(Lifecycle.EventType.EVENT_TYPE_STOP))));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdatePodStartLifecycleEvent() {
    assertDoesNotThrow(()
                           -> instanceInfoTimescaleDAO.updatePodLifecycleEvent(
                               "", ImmutableList.of(createLifecycleEvent(Lifecycle.EventType.EVENT_TYPE_START))));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdateNodeStopLifecycleEvent() {
    assertDoesNotThrow(()
                           -> instanceInfoTimescaleDAO.updateNodeLifecycleEvent(
                               "", ImmutableList.of(createLifecycleEvent(Lifecycle.EventType.EVENT_TYPE_STOP))));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdateNodeStartLifecycleEvent() {
    assertDoesNotThrow(()
                           -> instanceInfoTimescaleDAO.updateNodeLifecycleEvent(
                               "", ImmutableList.of(createLifecycleEvent(Lifecycle.EventType.EVENT_TYPE_START))));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testUpdateNodeStopEvent() {
    assertDoesNotThrow(() -> instanceInfoTimescaleDAO.updateNodeStopEvent(ImmutableList.of(createInstanceEvent())));
  }

  private static void assertDoesNotThrow(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception ex) {
      log.error("ERROR: ", ex);
      Assert.fail();
    }
  }

  private static Lifecycle createLifecycleEvent(Lifecycle.EventType eventType) {
    return Lifecycle.newBuilder().setType(eventType).build();
  }

  private static InstanceEvent createInstanceEvent() {
    return InstanceEvent.builder().build();
  }

  private static K8sWorkloadSpec createK8sWorkloadSpec() {
    return K8sWorkloadSpec.newBuilder().build();
  }

  private static InstanceInfo createInstanceInfo() {
    return InstanceInfo.builder().build();
  }
}
