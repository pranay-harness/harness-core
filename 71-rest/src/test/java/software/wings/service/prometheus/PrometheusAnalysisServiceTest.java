package software.wings.service.prometheus;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.impl.prometheus.PrometheusAnalysisServiceImpl;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse.PrometheusMetricData;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse.PrometheusMetricDataResult;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;

import java.io.IOException;
import java.util.Arrays;

public class PrometheusAnalysisServiceTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private PrometheusDelegateService prometheusDelegateService;
  @Mock private MLServiceUtils mlServiceUtils;
  @InjectMocks private PrometheusAnalysisServiceImpl prometheusAnalysisService;

  String settingId;

  @Before
  public void setup() {
    settingId = generateUuid();
    MockitoAnnotations.initMocks(this);
    when(delegateProxyFactory.get(any(), any())).thenReturn(prometheusDelegateService);
    when(mlServiceUtils.getHostNameFromExpression(any())).thenReturn("dummyHostName");
    PrometheusConfig config = PrometheusConfig.builder().build();
    when(settingsService.get(settingId))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(config).build());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetNodeDataCheckDelegateCalledTwice() throws Exception {
    PrometheusSetupTestNodeData nodeData = buildInput();
    prometheusAnalysisService.getMetricsWithDataForNode(nodeData);

    verify(prometheusDelegateService, times(2)).fetchMetricData(any(), anyString(), any(ThirdPartyApiCallLog.class));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetNodeData() throws Exception {
    PrometheusSetupTestNodeData nodeData = buildInput();

    when(prometheusDelegateService.fetchMetricData(any(), anyString(), any(ThirdPartyApiCallLog.class)))
        .thenReturn(createResponse());
    VerificationNodeDataSetupResponse setupResponse = prometheusAnalysisService.getMetricsWithDataForNode(nodeData);

    assertThat(setupResponse).isNotNull();
    assertThat(setupResponse.isProviderReachable()).isTrue();
    assertThat(setupResponse.getLoadResponse().isLoadPresent()).isTrue();
    assertThat(setupResponse.getDataForNode()).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetNodeData_withMultipleMetricValuesAreReturned() throws Exception {
    PrometheusSetupTestNodeData nodeData = buildInput();

    when(prometheusDelegateService.fetchMetricData(any(), anyString(), any(ThirdPartyApiCallLog.class)))
        .thenReturn(createResponseWithMultipleMetricsResponses());
    assertThatThrownBy(() -> prometheusAnalysisService.getMetricsWithDataForNode(nodeData))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetNodeDataBadHostCall() throws Exception {
    PrometheusSetupTestNodeData nodeData = buildInput();

    when(prometheusDelegateService.fetchMetricData(any(), anyString(), any(ThirdPartyApiCallLog.class)))
        .thenReturn(createResponse())
        .thenThrow(new IOException("Exception during the host call"));
    VerificationNodeDataSetupResponse setupResponse = prometheusAnalysisService.getMetricsWithDataForNode(nodeData);

    assertThat(setupResponse).isNotNull();
    assertThat(setupResponse.isProviderReachable()).isTrue();
    assertThat(setupResponse.getLoadResponse().isLoadPresent()).isTrue();
    assertThat(setupResponse.getDataForNode()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetNotReachable() throws Exception {
    PrometheusSetupTestNodeData nodeData = buildInput();

    when(prometheusDelegateService.fetchMetricData(any(), anyString(), any(ThirdPartyApiCallLog.class)))
        .thenThrow(new IOException("unsuccessful call"));
    VerificationNodeDataSetupResponse setupResponse = prometheusAnalysisService.getMetricsWithDataForNode(nodeData);

    assertThat(setupResponse).isNotNull();
    assertThat(setupResponse.isProviderReachable()).isFalse();
  }

  private PrometheusMetricDataResponse createResponse() {
    PrometheusMetricDataResult result = PrometheusMetricDataResult.builder().build();

    PrometheusMetricData data = PrometheusMetricData.builder().build();
    data.setResult(Arrays.asList(result));

    PrometheusMetricDataResponse response = PrometheusMetricDataResponse.builder().build();
    response.setStatus("success");
    response.setData(data);
    return response;
  }

  private PrometheusMetricDataResponse createResponseWithMultipleMetricsResponses() {
    PrometheusMetricDataResult result = PrometheusMetricDataResult.builder().build();

    PrometheusMetricData data = PrometheusMetricData.builder().build();
    data.setResult(Arrays.asList(result, result));

    PrometheusMetricDataResponse response = PrometheusMetricDataResponse.builder().build();
    response.setStatus("success");
    response.setData(data);
    return response;
  }

  private PrometheusSetupTestNodeData buildInput() {
    return PrometheusSetupTestNodeData.builder()
        .timeSeriesToAnalyze(Arrays.asList(
            TimeSeries.builder()
                .metricName("cpu")
                .metricType("INFRA")
                .txnName("Hardware")
                .url(
                    "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=container_cpu_usage_seconds_total{container_name=\"harness-example\",pod_name=\"$hostName\"}")
                .build()))
        .instanceElement(InstanceElement.Builder.anInstanceElement().hostName("dummyHostName").build())
        .settingId(settingId)
        .build();
  }
}
