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

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.OrchestrationStepsTest;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.rule.Owner;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class ResourceRestraintPersistenceMonitorTest extends OrchestrationStepsTest {
  @Mock private PersistenceIteratorFactory persistenceIteratorFactory;
  @Mock private ResourceRestraintService resourceRestraintService;
  @Inject @InjectMocks private ResourceRestraintPersistenceMonitor persistenceMonitor;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    persistenceMonitor.registerIterators();
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
    verify(resourceRestraintService).updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintService, never()).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testActiveInstance() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    when(resourceRestraintService.updateActiveConstraintsForInstance(instance)).thenReturn(true);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintService).updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintService).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testActiveInstance_WhenNoInstancesAreUpdated() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    when(resourceRestraintService.updateActiveConstraintsForInstance(instance)).thenReturn(false);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintService, never())
        .updateBlockedConstraints(Sets.newHashSet(instance.getResourceRestraintId()));
    verify(resourceRestraintService).updateActiveConstraintsForInstance(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowWingsException() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    doThrow(new WingsException("exception"))
        .when(resourceRestraintService)
        .updateActiveConstraintsForInstance(instance);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintService).updateActiveConstraintsForInstance(any());
    verify(resourceRestraintService, never()).updateBlockedConstraints(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowRuntimeException() {
    ResourceRestraintInstance instance = getResourceRestraint(ACTIVE);
    doThrow(new RuntimeException("exception"))
        .when(resourceRestraintService)
        .updateActiveConstraintsForInstance(instance);
    persistenceMonitor.handle(instance);
    verify(resourceRestraintService).updateActiveConstraintsForInstance(any());
    verify(resourceRestraintService, never()).updateBlockedConstraints(any());
  }

  private ResourceRestraintInstance getResourceRestraint(State state) {
    return ResourceRestraintInstance.builder().resourceRestraintId(generateUuid()).state(state).build();
  }
}
