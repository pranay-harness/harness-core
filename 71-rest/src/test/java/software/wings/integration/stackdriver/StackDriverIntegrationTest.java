package software.wings.integration.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import io.harness.beans.ExecutionStatus;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RepeatRule.Repeat;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
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
  private String GOOGLE_ACCOUNT = "harness-exploration";

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
    workflowId = wingsPersistence.save(aWorkflow().withAppId(appId).withName(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        aWorkflowExecution().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .withExecutionUuid(workflowExecutionId)
                              .withStateType(StateType.PHASE.name())
                              .withAppId(appId)
                              .withDisplayName(generateUuid())
                              .build());
    SettingAttribute settingAttribute = settingsService.getByName(accountId, Application.GLOBAL_APP_ID, GOOGLE_ACCOUNT);
    assertNotNull(settingAttribute);
    gcpConfigId = settingAttribute.getUuid();
    assertTrue(isNotEmpty(gcpConfigId));
  }

  @Test
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Owner(emails = "pranjal@harness.io", intermittent = true)
  public void testGetMetricsWithDataForNode() throws Exception {
    StackDriverSetupTestNodeData setupTestNodedata = getStackDriverSetupTestNodedata();
    WebTarget target = client.target(API_BASE + "/stackdriver/node-data?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodedata, MediaType.APPLICATION_JSON));

    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  private StackDriverSetupTestNodeData getStackDriverSetupTestNodedata() {
    return StackDriverSetupTestNodeData.builder()
        .appId(appId)
        .settingId(gcpConfigId)
        .loadBalancerMetrics(getMockLBMetrics())
        .vmInstanceMetrics(getMockVMMetrics())
        .instanceName("testHost")
        .toTime(System.currentTimeMillis())
        .fromTime(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30))
        .instanceElement(
            anInstanceElement()
                .withUuid("8cec1e1b0d16")
                .withDisplayName("8cec1e1b0d16")
                .withHostName("testHost")
                .withDockerId("8cec1e1b0d16")
                .withHost(aHostElement()
                              .withUuid("8cec1e1b0d16")
                              .withHostName("testHost")
                              .withIp("1.1.1.1")
                              .withInstanceId(null)
                              .withPublicDns(null)
                              .withEc2Instance(null)
                              .build())
                .withServiceTemplateElement(aServiceTemplateElement().withUuid("8cec1e1b0d16").withName(null).build())
                .withPodName("testHost")
                .withWorkloadName("testHost")
                .build())
        .workflowId(workflowId)
        .build();
  }

  private Set<StackDriverMetric> getMockVMMetrics() {
    Set<StackDriverMetric> metrics = new HashSet<>();
    StackDriverMetric vmInstanceMetric = new StackDriverMetric();
    vmInstanceMetric.setMetricName("compute.googleapis.com/instance/cpu/utilization");
    vmInstanceMetric.setDisplayName("CPU Utilization");
    vmInstanceMetric.setUnit("ratio");
    vmInstanceMetric.setKind("Gauge");
    vmInstanceMetric.setValueType("Double");
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
