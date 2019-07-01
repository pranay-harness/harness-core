package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrometheusStateTest extends APMStateVerificationTestBase {
  @InjectMocks private PrometheusState prometheusState;
  @Mock SettingsService settingsService;
  @Mock MetricDataAnalysisService metricAnalysisService;
  @Mock AppService appService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock DelegateService delegateService;

  @Before
  public void setup() throws Exception {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();
  }

  @Test
  @Category(UnitTests.class)
  public void testDefaultComparisionStrategy() {
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, prometheusState.getComparisonStrategy());
  }

  @Test
  @Category(UnitTests.class)
  public void testRenderURLExpression() throws IllegalAccessException {
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    VerificationStateAnalysisExecutionData executionData = mock(VerificationStateAnalysisExecutionData.class);
    Map<String, String> hosts = new HashMap<>();
    hosts.put("prometheus.host", "default");

    when(settingsService.get(any())).thenReturn(mock(SettingAttribute.class));
    when(appService.get(anyString())).thenReturn(application);

    String renderedUrl = "http://localhost:9090?test=test_value";
    String testUrl = "http://localhost:9090?test={$TEST_VAR}";
    List<TimeSeries> timeSeriesToAnalyze = new ArrayList<>();
    TimeSeries timeSeries =
        TimeSeries.builder().metricName("testMetric").url(testUrl).metricType(MetricType.INFRA.name()).build();
    timeSeriesToAnalyze.add(timeSeries);
    FieldUtils.writeField(prometheusState, "timeSeriesToAnalyze", timeSeriesToAnalyze, true);

    when(executionContext.renderExpression(testUrl)).thenReturn(renderedUrl);

    prometheusState.triggerAnalysisDataCollection(executionContext, analysisContext, executionData, hosts);
    ArgumentCaptor<DelegateTask> argument = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(argument.capture());
    TaskData taskData = argument.getValue().getData();
    Object parameters[] = taskData.getParameters();
    assertEquals(parameters.length, 1);
    assertEquals(taskData.getTaskType(), TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK.name());
    PrometheusDataCollectionInfo prometheusDataCollectionInfo = (PrometheusDataCollectionInfo) parameters[0];
    assertEquals(renderedUrl, prometheusDataCollectionInfo.getTimeSeriesToCollect().get(0).getUrl());
  }
}
