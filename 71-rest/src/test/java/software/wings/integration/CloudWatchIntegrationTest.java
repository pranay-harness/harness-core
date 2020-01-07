package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import software.wings.api.HostElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collections;
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
  public static final String AWS_PLAY_GROUND = "aws-playground";
  public static final String AWS_PLAY_GROUND_NO_LAMBDA = "aws-playground_no_lambda";
  private String awsConfigId;
  private String awsConfigNoLambdaId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    workflowId = wingsPersistence.save(aWorkflow().appId(appId).name(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecution.builder().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    UsageRestrictions defaultUsageRestrictions = getAllAppAllEnvUsageRestrictions();

    SettingAttribute awsNonProdAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AWS_PLAY_GROUND)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(accountId)
                           .build())
            .withUsageRestrictions(defaultUsageRestrictions)
            .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(awsNonProdAttribute));

    SettingAttribute awsNonLambdaProdAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(AWS_PLAY_GROUND_NO_LAMBDA)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(
                AwsConfig.builder()
                    .accessKey(scmSecret.decryptToString(new SecretName("aws_playground_no_lambda_access_key")))
                    .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_no_lambda_secret_key")))
                    .accountId(accountId)
                    .build())
            .withUsageRestrictions(defaultUsageRestrictions)
            .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(awsNonLambdaProdAttribute));

    wingsPersistence.save(aStateExecutionInstance()
                              .executionUuid(workflowExecutionId)
                              .stateType(StateType.PHASE.name())
                              .appId(appId)
                              .displayName(generateUuid())
                              .build());
    SettingAttribute settingAttribute =
        settingsService.getByName(accountId, Application.GLOBAL_APP_ID, AWS_PLAY_GROUND);
    assertThat(settingAttribute).isNotNull();
    awsConfigId = settingAttribute.getUuid();

    SettingAttribute settingAttribute1 =
        settingsService.getByName(accountId, Application.GLOBAL_APP_ID, AWS_PLAY_GROUND_NO_LAMBDA);
    assertThat(settingAttribute1).isNotNull();
    awsConfigNoLambdaId = settingAttribute1.getUuid();

    assertThat(isNotEmpty(awsConfigId)).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetEc2Metrics() throws Exception {
    WebTarget target = client.target(
        API_BASE + "/cloudwatch/get-metric-names?accountId=" + accountId + "&awsNameSpace=" + AwsNameSpace.EC2);
    RestResponse<List<CloudWatchMetric>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CloudWatchMetric>>>() {});
    assertThat(restResponse.getResource().size() > 0).isTrue();
  }

  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetLoadBalancersTest() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-load-balancers?accountId=" + accountId
        + "&settingId=" + awsConfigId + "&region=" + Regions.US_EAST_1.getName());
    RestResponse<Set<String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Set<String>>>() {});
    assertThat(restResponse.getResource().size() > 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetMetricsWithDataForNode() throws Exception {
    CloudWatchSetupTestNodeData setupTestNodedata = getCloudWatchSetupTestNodedata();
    WebTarget target = client.target(API_BASE + "/cloudwatch/node-data?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodedata, MediaType.APPLICATION_JSON));

    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testGetLambdaFunctionNamesPermissionsNotAvailable() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-lambda-functions?accountId=" + accountId
        + "&settingId=" + awsConfigNoLambdaId + "&region=" + Regions.US_EAST_1.getName());
    thrown.expect(Exception.class);
    getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<String>>>() {});
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetECSClusternNames() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-ecs-cluster-names?accountId=" + accountId
        + "&settingId=" + awsConfigId + "&region=" + Regions.US_EAST_1.getName());
    RestResponse<List<String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<String>>>() {});

    List<String> ecsClusterNames = restResponse.getResource();

    assertThat(ecsClusterNames.size() > 1).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetEC2InstancesNames() throws Exception {
    WebTarget target = client.target(API_BASE + "/cloudwatch/get-ec2-instances?accountId=" + accountId
        + "&settingId=" + awsConfigId + "&region=" + Regions.US_EAST_1.getName());
    RestResponse<Map<String, String>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Map<String, String>>>() {});

    Map<String, String> ec2InstanceIdByDnsName = restResponse.getResource();

    assertThat(ec2InstanceIdByDnsName.size() > 1).isTrue();
  }

  private CloudWatchSetupTestNodeData getCloudWatchSetupTestNodedata() {
    return CloudWatchSetupTestNodeData.builder()
        .region("us-east-2")
        .appId(appId)
        .settingId(awsConfigId)
        .loadBalancerMetricsByLBName(getMockMetricsByLBName())
        .ecsMetrics(getMockMetricsByECSName())
        .ec2Metrics(getMockEC2Metrics())
        .guid("test_guid")
        .instanceName("testHost")
        .toTime(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1))
        .fromTime(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)
            - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1))
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
                                   .statistics("Average")
                                   .enabledDefault(true)
                                   .build();
    ec2Metrics.add(metric1);

    CloudWatchMetric metric2 = CloudWatchMetric.builder()
                                   .metricName("DiskReadBytes")
                                   .displayName("Disk Read Bytes")
                                   .dimension("InstanceId")
                                   .dimensionDisplay("Host name expression")
                                   .metricType("VALUE")
                                   .statistics("Average")
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
                                   .statistics("Average")
                                   .enabledDefault(true)
                                   .build();
    List<CloudWatchMetric> metrics = new ArrayList<>();
    metrics.add(metric1);
    metricsByLBName.put("int-test", metrics);
    return metricsByLBName;
  }

  private Map<String, List<CloudWatchMetric>> getMockMetricsByECSName() {
    Map<String, List<CloudWatchMetric>> metricsByLBName = new HashMap<>();
    CloudWatchMetric metric1 = CloudWatchMetric.builder()
                                   .metricName("CPUUtilization")
                                   .displayName("CPU Utilization")
                                   .dimension("ClusterName")
                                   .dimensionDisplay("Cluster Name")
                                   .metricType("VALUE")
                                   .statistics("Average")
                                   .enabledDefault(true)
                                   .build();
    List<CloudWatchMetric> metrics = new ArrayList<>();
    metrics.add(metric1);
    metricsByLBName.put("CV-ECS-QA-Test", metrics);
    return metricsByLBName;
  }
}
