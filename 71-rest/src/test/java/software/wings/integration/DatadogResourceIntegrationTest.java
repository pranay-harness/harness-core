package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
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
  @Category(IntegrationTests.class)
  public void testGetTimeseriesRecordsForWorkflow() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(true);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + settingId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetTimeseriesRecordsForServiceGuard() {
    DataDogSetupTestNodeData fetchConfig = getDatadogSetupTestNodedata(false);

    WebTarget target = client.target(API_BASE + "/"
        + "datadog" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + settingId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(fetchConfig, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  private DataDogSetupTestNodeData getDatadogSetupTestNodedata(boolean isWorkflowConfig) {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    long fromTime = toTime - TimeUnit.MINUTES.toMillis(20) / TimeUnit.SECONDS.toMillis(1);

    DataDogSetupTestNodeData dataDogSetupTestNodeData =
        DataDogSetupTestNodeData.builder()
            .appId(appId)
            .settingId(settingId)
            .instanceName("testHost")
            .deploymentType(DeploymentType.KUBERNETES.name())
            .toTime(toTime)
            .fromTime(fromTime)
            .instanceElement(anInstanceElement()
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
                                 .withServiceTemplateElement(
                                     aServiceTemplateElement().withUuid("8cec1e1b0d16").withName(null).build())
                                 .withPodName("testHost")
                                 .withWorkloadName("testHost")
                                 .build())
            .workflowId(workflowId)
            .build();

    if (isWorkflowConfig) {
      dataDogSetupTestNodeData.setMetrics("docker.cpu.usage,docker.mem.rss");
      dataDogSetupTestNodeData.setDatadogServiceName("test");
    } else {
      Map<String, String> dockerMetrics = new HashMap<>();
      dockerMetrics.put("cluster-name:harness-test", "docker.cpu.usage,docker.mem.rss");
      dataDogSetupTestNodeData.setDockerMetrics(dockerMetrics);

      Map<String, String> ecsMetrics = new HashMap<>();
      ecsMetrics.put("cluster_name:sdktesting", "ecs.fargate.cpu.user");
      dataDogSetupTestNodeData.setEcsMetrics(ecsMetrics);

      dataDogSetupTestNodeData.setServiceLevel(true);
    }
    return dataDogSetupTestNodeData;
  }
}