package software.wings.integration;

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

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.DeploymentType;
import software.wings.beans.DatadogConfig;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.datadog.DataDogSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 10/22/2018
 */
public class DatadogResourceIntegrationTest extends BaseIntegrationTest {
  private String settingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;

  @Inject private ScmSecret scmSecret;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    settingId = wingsPersistence.save(
        Builder.aSettingAttribute()
            .withName(generateUuid())
            .withAccountId(accountId)
            .withValue(DatadogConfig.builder()
                           .url("https://app.datadoghq.com/api/v1/")
                           .apiKey(scmSecret.decryptToCharArray(new SecretName("datadog_api_key")))
                           .applicationKey(scmSecret.decryptToCharArray(new SecretName("datadog_application_key")))
                           .accountId(accountId)
                           .build())
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
  @Category(IntegrationTests.class)
  @Ignore("Disabled due to rate limit issue on Datadog")
  public void testGetTimeseriesRecordsForWorkflowWithoutServerConfigId() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(true, StateType.DATA_DOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + null);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  @Ignore("Disabled due to rate limit issue on Datadog")
  public void testGetTimeseriesRecordsForWorkflow() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(true, StateType.DATA_DOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + settingId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Category(IntegrationTests.class)
  public void testGetTimeseriesRecordsForServiceGuard() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(false, StateType.DATA_DOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + settingId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  @Ignore("Disabled due to rate limit issue on Datadog")
  public void testGetTimeseriesRecordsForServiceGuardWithoutServerConfigId() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(false, StateType.DATA_DOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + null);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testDatadogLogsForWorkflow() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(true, StateType.DATA_DOG_LOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + null);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(response.getString("dataForNode").contains("logs")).isTrue();
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testDatadogLogsForServiceGuard() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(false, StateType.DATA_DOG_LOG);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + null);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(response.getString("dataForNode").contains("logs")).isTrue();
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  private DataDogSetupTestNodeData getDatadogSetupTestNodedata(boolean isWorkflowConfig, StateType stateType) {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    long fromTime = toTime - TimeUnit.MINUTES.toMillis(20) / TimeUnit.SECONDS.toMillis(1);

    DataDogSetupTestNodeData dataDogSetupTestNodeData = DataDogSetupTestNodeData.builder()
                                                            .stateType(stateType)
                                                            .appId(appId)
                                                            .settingId(settingId)
                                                            .instanceName("testHost")
                                                            .deploymentType(DeploymentType.KUBERNETES.name())
                                                            .toTime(toTime)
                                                            .fromTime(fromTime)
                                                            .workflowId(workflowId)
                                                            .build();

    if (isWorkflowConfig) {
      dataDogSetupTestNodeData.setInstanceElement(
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
              .build());
      if (stateType.equals(StateType.DATA_DOG)) {
        dataDogSetupTestNodeData.setMetrics("docker.cpu.usage,docker.mem.rss");
        dataDogSetupTestNodeData.setDatadogServiceName("test");
      } else {
        dataDogSetupTestNodeData.setQuery("exception");
        dataDogSetupTestNodeData.setHostNameField("pod");
      }
    } else {
      if (stateType.equals(StateType.DATA_DOG)) {
        Map<String, String> dockerMetrics = new HashMap<>();
        dockerMetrics.put("cluster-name:harness-test", "docker.cpu.usage,docker.mem.rss");
        dataDogSetupTestNodeData.setDockerMetrics(dockerMetrics);

        Map<String, String> ecsMetrics = new HashMap<>();
        ecsMetrics.put("cluster_name:sdktesting", "ecs.fargate.cpu.user");
        dataDogSetupTestNodeData.setEcsMetrics(ecsMetrics);
      } else {
        dataDogSetupTestNodeData.setQuery("exception");
      }
    }
    dataDogSetupTestNodeData.setServiceLevel(!isWorkflowConfig);
    return dataDogSetupTestNodeData;
  }
}