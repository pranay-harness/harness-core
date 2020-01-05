package software.wings.integration.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRANJAL;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 11/27/2018
 */
public class StackDriverIntegrationTest extends BaseIntegrationTest {
  private String gcpConfigId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  private static final String GCP_PLAYGROUND = "playground-gke-gcs-gcr";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    workflowId = wingsPersistence.save(aWorkflow().appId(appId).name(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecution.builder().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .executionUuid(workflowExecutionId)
                              .stateType(StateType.PHASE.name())
                              .appId(appId)
                              .displayName(generateUuid())
                              .build());
    SettingAttribute settingAttribute = settingsService.getByName(accountId, Application.GLOBAL_APP_ID, GCP_PLAYGROUND);
    assertThat(settingAttribute).isNotNull();
    gcpConfigId = settingAttribute.getUuid();
    assertThat(isNotEmpty(gcpConfigId)).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetMetricsWithDataForNode() throws Exception {
    StackDriverSetupTestNodeData setupTestNodedata = getStackDriverSetupTestNodedata();
    WebTarget target = client.target(API_BASE + "/stackdriver/node-data?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodedata, MediaType.APPLICATION_JSON));

    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetRegionsForStackdriver() throws Exception {
    WebTarget target =
        client.target(API_BASE + "/stackdriver/get-regions?accountId=" + accountId + "&settingId=" + gcpConfigId);
    RestResponse<List<String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<String>>>() {});

    assertThat(restResponse.getResource().size() > 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetLoadBalancersForStackdriver() throws Exception {
    String region = "us-central1";
    WebTarget target = client.target(API_BASE + "/stackdriver/get-load-balancers?accountId=" + accountId
        + "&settingId=" + gcpConfigId + "&region=" + region);
    RestResponse<Map<String, String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Map<String, String>>>() {});

    assertThat(restResponse.getResource().size() > 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetSampleRecord() throws Exception {
    String guid = generateUuid();
    StackDriverSetupTestNodeData setupTestNodedata = StackDriverSetupTestNodeData.builder()
                                                         .appId(appId)
                                                         .settingId(gcpConfigId)
                                                         .query("exception")
                                                         .guid(guid)
                                                         .build();
    WebTarget target = client.target(API_BASE + "/stackdriver/get-sample-record?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodedata, MediaType.APPLICATION_JSON));

    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(response.get("severity")).isNotNull();
    assertThat(response.get("textPayload")).isNotNull();
  }

  private StackDriverSetupTestNodeData getStackDriverSetupTestNodedata() {
    return StackDriverSetupTestNodeData.builder()
        .appId(appId)
        .settingId(gcpConfigId)
        .loadBalancerMetrics(getMockLBMetrics())
        .instanceName("testHost")
        .toTime(System.currentTimeMillis() / 1000)
        .fromTime((System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) / 1000)
        .instanceElement(
            anInstanceElement()
                .uuid("8cec1e1b0d16")
                .displayName("8cec1e1b0d16")
                .hostName("testHost")
                .dockerId("8cec1e1b0d16")
                .host(aHostElement()
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
        .build();
  }

  private Set<StackDriverMetric> getMockVMMetrics() {
    Set<StackDriverMetric> metrics = new HashSet<>();
    StackDriverMetric vmInstanceMetric = new StackDriverMetric();
    vmInstanceMetric.setMetricName("kubernetes.io/container/memory/request_utilization");
    vmInstanceMetric.setMetric("MemoryRequestUtilization");
    vmInstanceMetric.setDisplayName("Memory Request Utilization");
    vmInstanceMetric.setUnit("number");
    vmInstanceMetric.setKind("VALUE");
    vmInstanceMetric.setValueType("Int64");
    metrics.add(vmInstanceMetric);
    return metrics;
  }

  private Map<String, List<StackDriverMetric>> getMockLBMetrics() {
    Map<String, List<StackDriverMetric>> metricsByRuleName = new HashMap<>();
    StackDriverMetric lbMetric = new StackDriverMetric();
    lbMetric.setMetricName("loadbalancing.googleapis.com/https/backend_latencies");
    lbMetric.setDisplayName("Backend Latencies");
    lbMetric.setUnit("ms");
    lbMetric.setKind("Delta");
    lbMetric.setValueType("Distribution");
    List<StackDriverMetric> metrics = new ArrayList<>();
    metrics.add(lbMetric);
    metricsByRuleName.put("http-endpoint", metrics);
    return metricsByRuleName;
  }
}
