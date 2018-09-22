package software.wings.integration;

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
import static software.wings.integration.DataGenUtil.AWS_PLAY_GROUND;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.rule.RepeatRule.Repeat;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by rsingh on 5/3/18.
 */
public class CloudWatchIntegrationTest extends BaseIntegrationTest {
  @Inject private SettingsService settingsService;

  private String awsConfigId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
    workflowId = wingsPersistence.save(aWorkflow().withAppId(appId).withName(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).withStatus(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .withExecutionUuid(workflowExecutionId)
                              .withStateType(StateType.PHASE.name())
                              .withAppId(appId)
                              .withDisplayName(generateUuid())
                              .build());
    SettingAttribute settingAttribute =
        settingsService.getByName(accountId, Application.GLOBAL_APP_ID, AWS_PLAY_GROUND);
    assertNotNull(settingAttribute);
    awsConfigId = settingAttribute.getUuid();
    assertTrue(isNotEmpty(awsConfigId));
  }

  @Test
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  public void testGetEc2Metrics() throws Exception {
    WebTarget target = client.target(
        API_BASE + "/cloudwatch/get-metric-names?accountId=" + accountId + "&awsNameSpace=" + AwsNameSpace.EC2);
    RestResponse<List<CloudWatchMetric>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CloudWatchMetric>>>() {});
    assertTrue(restResponse.getResource().size() > 0);
  }

  @Test
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  public void testGetLoadBalancersTest() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-load-balancers?accountId=" + accountId
        + "&settingId=" + awsConfigId + "&region=" + Regions.US_EAST_1.getName());
    RestResponse<Set<String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Set<String>>>() {});
    assertTrue(restResponse.getResource().size() > 0);
  }

  @Test
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  public void testGetMetricsWithDataForNode() throws Exception {
    CloudWatchSetupTestNodeData setupTestNodedata = getCloudWatchSetupTestNodedata();
    WebTarget target = client.target(API_BASE + "/cloudwatch/node-data?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodedata, MediaType.APPLICATION_JSON));

    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  private CloudWatchSetupTestNodeData getCloudWatchSetupTestNodedata() {
    return CloudWatchSetupTestNodeData.builder()
        .region("us-east-2")
        .appId(appId)
        .settingId(awsConfigId)
        .loadBalancerMetricsByLBName(getMockMetricsByLBName())
        .ec2Metrics(getMockEC2Metrics())
        .instanceName("testHost")
        .toTime(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1))
        .fromTime(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)
            - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1))
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

  private List<CloudWatchMetric> getMockEC2Metrics() {
    List<CloudWatchMetric> ec2Metrics = new ArrayList<>();
    CloudWatchMetric metric1 = CloudWatchMetric.builder()
                                   .metricName("CPUUtilization")
                                   .displayName("CPU Usage")
                                   .dimension("InstanceId")
                                   .dimensionDisplay("Host name expression")
                                   .metricType("VALUE")
                                   .enabledDefault(true)
                                   .build();
    ec2Metrics.add(metric1);

    CloudWatchMetric metric2 = CloudWatchMetric.builder()
                                   .metricName("DiskReadBytes")
                                   .displayName("Disk Read Bytes")
                                   .dimension("InstanceId")
                                   .dimensionDisplay("Host name expression")
                                   .metricType("VALUE")
                                   .enabledDefault(true)
                                   .build();
    ec2Metrics.add(metric2);
    return ec2Metrics;
  }

  private Map<String, List<CloudWatchMetric>> getMockMetricsByLBName() {
    Map<String, List<CloudWatchMetric>> metricsByLBName = new HashMap<>();
    CloudWatchMetric metric1 = CloudWatchMetric.builder()
                                   .metricName("Latency")
                                   .displayName("Latency")
                                   .dimension("LoadBalancerName")
                                   .dimensionDisplay("Load balancer name")
                                   .metricType("ERROR")
                                   .enabledDefault(true)
                                   .build();
    List<CloudWatchMetric> metrics = new ArrayList<>();
    metrics.add(metric1);
    metricsByLBName.put("int-test", metrics);
    return metricsByLBName;
  }
}
