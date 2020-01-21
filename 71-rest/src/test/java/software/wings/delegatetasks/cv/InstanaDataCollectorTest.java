package software.wings.delegatetasks.cv;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.InstanaConfig;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaInfraMetrics;
import software.wings.service.intfc.instana.InstanaDelegateService;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class InstanaDataCollectorTest extends WingsBaseTest {
  private InstanaConfig config;
  private InstanaDataCollector instanaDataCollector;
  private String host = "test-host";
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Mock DataCollectionExecutionContext dataCollectionExecutionContext;
  @Mock InstanaDelegateService instanaDelegateService;
  private String accountId = generateUuid();

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    initMocks(this);
    config = InstanaConfig.builder()
                 .instanaUrl("https://instana-example.com/")
                 .accountId(accountId)
                 .apiToken(UUID.randomUUID().toString().toCharArray())
                 .build();
    instanaDataCollector = new InstanaDataCollector();
    FieldUtils.writeField(instanaDataCollector, "instanaDelegateService", instanaDelegateService, true);
    when(dataCollectionExecutionContext.getActivityLogger())
        .thenReturn(mock(DelegateCVActivityLogService.Logger.class));
    ThirdPartyApiCallLog thirdPartyApiCallLog = mock(ThirdPartyApiCallLog.class);
    when(dataCollectionExecutionContext.createApiCallLog()).thenReturn(thirdPartyApiCallLog);
    when(dataCollectionExecutionContext.executeRequest(any(), any())).then(invocation -> {
      String title = invocation.getArgumentAt(0, String.class);
      Call<?> call = invocation.getArgumentAt(1, Call.class);
      Response<?> response = call.execute();
      return response.body();
    });
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testInit_failIfInvalidQuery() {
    InstanaDataCollectionInfo instanaDataCollectionInfo = createDataCollectionInfo();
    instanaDataCollectionInfo.setQuery("InvalidQueryWithoutHostExpression");
    instanaDataCollector.init(dataCollectionExecutionContext, instanaDataCollectionInfo);
    assertThatThrownBy(() -> instanaDataCollector.fetchMetrics(Lists.newArrayList(host)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Query should contain ${host.hostName}");
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHostBatchSize() {
    assertThat(instanaDataCollector.getHostBatchSize()).isEqualTo(1);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFetchMetric_withHostWithEmptyItems() {
    instanaDataCollector.init(dataCollectionExecutionContext, createDataCollectionInfo());
    InstanaInfraMetrics instanaInfraMetrics = getInstanaInfraMetricsEmptyItems();
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any())).thenReturn(instanaInfraMetrics);
    List<MetricElement> metricElements = instanaDataCollector.fetchMetrics(Lists.newArrayList(host));
    assertThat(metricElements).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFetchMetric_withHostWithEmptyData() {
    instanaDataCollector.init(dataCollectionExecutionContext, createDataCollectionInfo());
    InstanaInfraMetrics instanaInfraMetrics = getInstanaInfraMetricsEmptyResponse();
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any())).thenReturn(instanaInfraMetrics);
    List<MetricElement> metricElements = instanaDataCollector.fetchMetrics(Lists.newArrayList(host));
    assertThat(metricElements).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFetchMetric_withHost() {
    instanaDataCollector.init(dataCollectionExecutionContext, createDataCollectionInfo());
    InstanaInfraMetrics instanaInfraMetrics = getInstanaMetrics();
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any())).thenReturn(instanaInfraMetrics);
    List<MetricElement> metricElements = instanaDataCollector.fetchMetrics(Lists.newArrayList(host));
    assertThat(metricElements).hasSize(1);
    String expected = "[\n"
        + "  {\n"
        + "    \"name\": \"Infrastructure\",\n"
        + "    \"host\": \"test-host\",\n"
        + "    \"groupName\": \"default\",\n"
        + "    \"timestamp\": 1579508880000,\n"
        + "    \"values\": {\n"
        + "      \"memory_usage\": 246259780.2666,\n"
        + "      \"cpu_total_usage\": 0.1265\n"
        + "    }\n"
        + "  }\n"
        + "]";
    assertThat(metricElements).isEqualTo(JsonUtils.asObject(expected, List.class, MetricElement.class));
  }

  private InstanaInfraMetrics getInstanaInfraMetricsEmptyResponse() {
    String json = "{\n"
        + "    \"items\": [\n"
        + "        {\n"
        + "            \"snapshotId\": \"-Ag3ICIk3z23l4HShQQCWt9KkYQ\",\n"
        + "            \"plugin\": \"docker\",\n"
        + "            \"from\": 1579157474000,\n"
        + "            \"to\": 1579161206000,\n"
        + "            \"tags\": [],\n"
        + "            \"label\": \"hs-harness-todolist-hs (default/cv-pr-kube-kube-v1-service-kube-env-1-9c7c76f7c-sz7n4)\",\n"
        + "            \"host\": \"GCE-3791764252540429558\",\n"
        + "            \"metrics\": {\n"
        + "                \"memory.usage\": [],\n"
        + "                \"cpu.total_usage\": []\n"
        + "            }\n"
        + "        }\n"
        + "    ]\n"
        + "}";
    return JsonUtils.asObject(json, InstanaInfraMetrics.class);
  }

  private InstanaInfraMetrics getInstanaMetrics() {
    String jsonResponse = "{\n"
        + "  \"items\": [\n"
        + "    {\n"
        + "      \"from\": 1579508772000,\n"
        + "      \"to\": 0,\n"
        + "      \"label\": \"hs-harness-todolist-hs (default/cv-pr-kube-kube-v1-appdyn-kube-env-3-6c6c8c94f4-m9fq4)\",\n"
        + "      \"metrics\": {\n"
        + "        \"memory.usage\": [\n"
        + "          [\n"
        + "            1579508880000,\n"
        + "            246259780.2666\n"
        + "          ]\n"
        + "        ],\n"
        + "        \"cpu.total_usage\": [\n"
        + "          [\n"
        + "            1579508880000,\n"
        + "            0.1265\n"
        + "          ]\n"
        + "        ]\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}";
    return JsonUtils.asObject(jsonResponse, InstanaInfraMetrics.class);
  }
  private InstanaInfraMetrics getInstanaInfraMetricsEmptyItems() {
    String json = "{\n"
        + "    \"items\": [\n"
        + "    ]\n"
        + "}";
    return JsonUtils.asObject(json, InstanaInfraMetrics.class);
  }

  private InstanaDataCollectionInfo createDataCollectionInfo() {
    List<String> metrics = Lists.newArrayList("cpu.total_usage", "memory.usage");
    return InstanaDataCollectionInfo.builder()
        .query("entity.kubernetes.pod.name:${host.hostName}")
        .metrics(metrics)
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .instanaConfig(config)
        .hostsToGroupNameMap(Collections.singletonMap(host, "default"))
        .hosts(new HashSet<>(Arrays.asList(host)))
        .build();
  }
}