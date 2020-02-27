package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.PrometheusConfig;
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
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDefaultComparisionStrategy() {
    assertThat(prometheusState.getComparisonStrategy()).isEqualTo(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRenderURLExpression() throws IllegalAccessException {
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    VerificationStateAnalysisExecutionData executionData = mock(VerificationStateAnalysisExecutionData.class);
    Map<String, String> hosts = new HashMap<>();
    hosts.put("prometheus.host", "default");

    when(settingsService.get(any())).thenReturn(mock(SettingAttribute.class));
    when(appService.get(anyString())).thenReturn(application);

    String renderedUrl =
        "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=jvm_memory_max_bytes{pod_name=\"$hostName\"}";
    String testUrl = "jvm_memory_max_bytes{pod_name=\"$hostName\"}";
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
    assertThat(1).isEqualTo(parameters.length);
    assertThat(TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK.name()).isEqualTo(taskData.getTaskType());
    PrometheusDataCollectionInfo prometheusDataCollectionInfo = (PrometheusDataCollectionInfo) parameters[0];
    assertThat(prometheusDataCollectionInfo.getTimeSeriesToCollect().get(0).getUrl()).isEqualTo(renderedUrl);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection_renderConnectorId() throws IllegalAccessException {
    AnalysisContext analysisContext = mock(AnalysisContext.class);
    VerificationStateAnalysisExecutionData executionData = mock(VerificationStateAnalysisExecutionData.class);
    Map<String, String> hosts = new HashMap<>();
    hosts.put("prometheus.host", "default");
    String resolvedAnalysisServerConfigId = generateUuid();

    PrometheusConfig prometheusConfig = PrometheusConfig.builder().build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withUuid(resolvedAnalysisServerConfigId)
                                            .withValue(prometheusConfig)
                                            .withName("prometheus")
                                            .build();
    wingsPersistence.save(settingAttribute);

    when(settingsService.get(eq(resolvedAnalysisServerConfigId))).thenReturn(settingAttribute);
    when(appService.get(anyString())).thenReturn(application);
    String analysisServerConfigId = "${workflow.variables.connectorName}";
    prometheusState.setAnalysisServerConfigId(analysisServerConfigId);
    List<TimeSeries> timeSeriesToAnalyze = new ArrayList<>();
    String testUrl = "jvm_memory_max_bytes{pod_name=\"$hostName\"}";
    TimeSeries timeSeries =
        TimeSeries.builder().metricName("testMetric").url(testUrl).metricType(MetricType.INFRA.name()).build();
    timeSeriesToAnalyze.add(timeSeries);
    FieldUtils.writeField(prometheusState, "timeSeriesToAnalyze", timeSeriesToAnalyze, true);

    PrometheusState spyState = spy(prometheusState);
    when(spyState.getResolvedConnectorId(any(), eq("analysisServerConfigId"), eq(analysisServerConfigId)))
        .thenReturn(resolvedAnalysisServerConfigId);

    spyState.triggerAnalysisDataCollection(executionContext, analysisContext, executionData, hosts);
    ArgumentCaptor<DelegateTask> argument = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(argument.capture());
    TaskData taskData = argument.getValue().getData();
    Object parameters[] = taskData.getParameters();
    assertThat(1).isEqualTo(parameters.length);
    assertThat(TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK.name()).isEqualTo(taskData.getTaskType());
    PrometheusDataCollectionInfo prometheusDataCollectionInfo = (PrometheusDataCollectionInfo) parameters[0];
    assertThat(prometheusDataCollectionInfo.getPrometheusConfig()).isEqualTo(settingAttribute.getValue());
  }
}
