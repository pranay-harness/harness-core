/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.IteratorConfig;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.rule.Owner;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PIPELINE)
@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class ResourceRestraintPersistenceMonitorTest extends OrchestrationStepsTestBase {
  @Mock private PersistenceIteratorFactory persistenceIteratorFactory;
  @Mock private ResourceRestraintInstanceService resourceRestraintInstanceService;
  @Inject @InjectMocks private ResourceRestraintPersistenceMonitor persistenceMonitor;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    persistenceMonitor.registerIterators(
        IteratorConfig.builder().enabled(true).targetIntervalInSeconds(60).threadPoolCount(2).build());
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(PersistenceIteratorFactory.PumpExecutorOptions.class),
            eq(ResourceRestraintPersistenceMonitor.class), any(MongoPersistenceIteratorBuilder.class));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testBlockedInstance() {
    ResourceRestraintInstance instance = getResourceRestraint(BLOCKED);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintInstanceService)
        .updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintInstanceService, never()).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testActiveInstance() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    when(resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance)).thenReturn(true);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintInstanceService)
        .updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintInstanceService).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testActiveInstance_WhenNoInstancesAreUpdated() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    when(resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance)).thenReturn(false);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintInstanceService, never())
        .updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintInstanceService).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowWingsException() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    doThrow(new WingsException("exception"))
        .when(resourceRestraintInstanceService)
        .updateActiveConstraintsForInstance(instance);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintInstanceService).updateActiveConstraintsForInstance(any());
    verify(resourceRestraintInstanceService, never()).updateBlockedConstraints(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowRuntimeException() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    doThrow(new RuntimeException("exception"))
        .when(resourceRestraintInstanceService)
        .updateActiveConstraintsForInstance(instance);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintInstanceService).updateActiveConstraintsForInstance(any());
    verify(resourceRestraintInstanceService, never()).updateBlockedConstraints(any());
  }

  private ResourceRestraintInstance getResourceRestraint(State state) {
    return ResourceRestraintInstance.builder().resourceRestraintId(generateUuid()).state(state).build();
  }
}
