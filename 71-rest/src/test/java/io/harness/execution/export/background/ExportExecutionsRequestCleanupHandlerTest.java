package io.harness.execution.export.background;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.execution.export.request.ExportExecutionsRequest;
import io.harness.execution.export.request.RequestTestUtils;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class ExportExecutionsRequestCleanupHandlerTest extends WingsBaseTest {
  @Mock private PersistenceIteratorFactory mockPersistenceIteratorFactory;
  @Mock private ExportExecutionsService exportExecutionsService;
  @Inject @InjectMocks private ExportExecutionsRequestCleanupHandler exportExecutionsRequestCleanupHandler;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    exportExecutionsRequestCleanupHandler.registerIterators();
    verify(mockPersistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(PumpExecutorOptions.class),
            eq(ExportExecutionsRequestCleanupHandler.class), any(MongoPersistenceIteratorBuilder.class));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHandle() {
    exportExecutionsRequestCleanupHandler.handle(null);
    verify(exportExecutionsService, never()).expireRequest(any());

    exportExecutionsRequestCleanupHandler.handle(
        RequestTestUtils.prepareExportExecutionsRequest(ExportExecutionsRequest.Status.READY));
    verify(exportExecutionsService, times(1)).expireRequest(any());

    doThrow(new RuntimeException("")).when(exportExecutionsService).expireRequest(any());
    exportExecutionsRequestCleanupHandler.handle(
        RequestTestUtils.prepareExportExecutionsRequest(ExportExecutionsRequest.Status.READY));
    verify(exportExecutionsService, times(2)).expireRequest(any());
  }
}
