package io.harness.execution.export.background;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
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
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class ExportExecutionsRequestHandlerTest extends WingsBaseTest {
  @Mock private PersistenceIteratorFactory mockPersistenceIteratorFactory;
  @Mock private ExportExecutionsService exportExecutionsService;
  @Mock private ExportExecutionsNotificationHelper exportExecutionsNotificationHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private ExportExecutionsRequestHandler exportExecutionsRequestHandler;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    exportExecutionsRequestHandler.registerIterators();
    verify(mockPersistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(PumpExecutorOptions.class),
            eq(ExportExecutionsRequestHandler.class), any(MongoPersistenceIteratorBuilder.class));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHandle() {
    exportExecutionsRequestHandler.handle(null);
    verify(exportExecutionsService, never()).export(any());

    exportExecutionsRequestHandler.handle(RequestTestUtils.prepareExportExecutionsRequest());
    verify(exportExecutionsService, never()).export(any());
    verify(exportExecutionsService, times(1)).failRequest(any(), anyString());
    verify(exportExecutionsNotificationHelper, times(1)).dispatch(any());

    doThrow(new RuntimeException("")).when(exportExecutionsService).failRequest(any(), any());
    exportExecutionsRequestHandler.handle(RequestTestUtils.prepareExportExecutionsRequest());
    verify(exportExecutionsService, times(2)).failRequest(any(), anyString());
    verify(exportExecutionsNotificationHelper, times(1)).dispatch(any());

    enableFeatureFlag();
    exportExecutionsRequestHandler.handle(RequestTestUtils.prepareExportExecutionsRequest());
    verify(exportExecutionsService, times(1)).export(any());
    verify(exportExecutionsNotificationHelper, times(2)).dispatch(any());
  }

  private void enableFeatureFlag() {
    when(featureFlagService.isEnabled(eq(FeatureName.EXPORT_EXECUTION_LOGS), anyString())).thenReturn(true);
  }
}
