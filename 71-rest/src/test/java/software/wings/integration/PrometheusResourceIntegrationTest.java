package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRANJAL;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.HostElement;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 09/05/2018
 */
public class PrometheusResourceIntegrationTest extends BaseIntegrationTest {
  private String settingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    settingId = wingsPersistence.save(Builder.aSettingAttribute()
                                          .withName(generateUuid())
                                          .withAccountId(accountId)
                                          .withValue(new PrometheusConfig("http://35.247.2.110:8080", accountId))
                                          .build());

    workflowId = wingsPersistence.save(aWorkflow().appId(appId).name(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecution.builder().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .executionUuid(workflowExecutionId)
                              .stateType(StateType.PHASE.name())
                              .appId(appId)
                              .displayName(generateUuid())
                              .build());
  }

  @Test
  @Owner(developers = PRANJAL)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetMetricsWithDataForNode() {
    PrometheusSetupTestNodeData setupTestNodeData = getPrometheusSetupTestNodedata();
    WebTarget target = client.target(API_BASE + "/"
        + "prometheus"
        + "/node-data"
        + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetMetricsWithDataForNodeServiceLevel() {
    PrometheusSetupTestNodeData setupTestNodeData = getPrometheusSetupTestNodedata();
    setupTestNodeData.getTimeSeriesToAnalyze().iterator().next().setUrl(
        "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=container_cpu_usage_seconds_total");
    setupTestNodeData.setServiceLevel(true);

    WebTarget target = client.target(API_BASE + "/"
        + "prometheus"
        + "/node-data"
        + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  private PrometheusSetupTestNodeData getPrometheusSetupTestNodedata() {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    long fromTime = toTime - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1);
    return PrometheusSetupTestNodeData.builder()
        .timeSeriesToAnalyze(getMockTimeSeriesData())
        .appId(appId)
        .settingId(settingId)
        .instanceName("testHost")
        .instanceElement(
            anInstanceElement()
                .uuid("8cec1e1b0d16")
                .displayName("8cec1e1b0d16")
                .hostName("testHost")
                .dockerId("8cec1e1b0d16")
                .host(HostElement.builder()
                          .uuid("8cec1e1b0d16")
                          .hostName("testHost")
                          .ip("1.1.1.1")
                          .instanceId(null)
                          .publicDns(null)
                          .ec2Instance(null)
                          .build())
                .serviceTemplateElement(aServiceTemplateElement().withUuid("8cec1e1b0d16").withName(null).build())
                .podName("testHost")
                .workloadName("testHost")
                .build())
        .workflowId(workflowId)
        .fromTime(fromTime)
        .toTime(toTime)
        .build();
  }

  private List<TimeSeries> getMockTimeSeriesData() {
    List<TimeSeries> mockTimeSeriesData = new ArrayList<>();

    TimeSeries t1 =
        TimeSeries.builder()
            .txnName("Hardware")
            .metricName("CPU")
            .url(
                "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=container_cpu_usage_seconds_total{pod_name=\"$hostName\"}")
            .build();

    mockTimeSeriesData.add(t1);
    return mockTimeSeriesData;
  }
}
