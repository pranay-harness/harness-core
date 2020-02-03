package io.harness.workers.background.critical.iterator;

import static io.harness.rule.OwnerRule.YOGESH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.utils.ResourceConstraintTestConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.net.*"})
public class ResourceConstraintBackupHandlerTest extends WingsBaseTest {
  @Mock private ResourceConstraintService mockResourceConstraintService;
  @Mock private PersistenceIteratorFactory mockPersistenceIteratorFactory;
  @InjectMocks @Inject private ResourceConstraintBackupHandler resourceConstraintBackupHandler;

  private ResourceConstraintInstance resourceConstraintInstance;

  @Before
  public void setUp() throws Exception {
    resourceConstraintInstance = ResourceConstraintInstance.builder()
                                     .resourceConstraintId(ResourceConstraintTestConstants.RESOURCE_CONSTRAINT_ID)
                                     .build();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    resourceConstraintBackupHandler.registerIterators();
    verify(mockPersistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(PumpExecutorOptions.class),
            eq(ResourceConstraintBackupHandler.class), any(MongoPersistenceIteratorBuilder.class));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCatchExceptionInUpdateResourceConstraint() {
    resourceConstraintInstance.setState(State.ACTIVE.name());
    doThrow(new WingsException("error"))
        .when(mockResourceConstraintService)
        .updateActiveConstraintForInstance(resourceConstraintInstance);
    resourceConstraintBackupHandler.handle(resourceConstraintInstance);
    verify(mockResourceConstraintService, never()).updateBlockedConstraints(any());

    doThrow(new RuntimeException())
        .when(mockResourceConstraintService)
        .updateActiveConstraintForInstance(resourceConstraintInstance);
    resourceConstraintBackupHandler.handle(resourceConstraintInstance);
    verify(mockResourceConstraintService, never()).updateBlockedConstraints(any());
  }
}