package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SplunkConfig;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretGenerator.SecretName;
import software.wings.service.impl.splunk.SplunkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 09/01/2018
 */
public class SplunkResourceIntegrationTest extends BaseIntegrationTest {
  private String elkSettingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  @Inject private SecretGenerator secretGenerator;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
    elkSettingId = wingsPersistence.save(
        Builder.aSettingAttribute()
            .withName(generateUuid())
            .withAccountId(accountId)
            .withValue(SplunkConfig.builder()
                           .splunkUrl("https://ec2-52-204-66-40.compute-1.amazonaws.com:8089")
                           .username("admin")
                           .password(secretGenerator.decryptToCharArray(new SecretName("splunk_config_password")))
                           .accountId(accountId)
                           .build())
            .build());

    workflowId = wingsPersistence.save(aWorkflow().withAppId(appId).withName(generateUuid()).build());
    workflowExecutionId = wingsPersistence.save(
        aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).withStatus(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .withExecutionUuid(workflowExecutionId)
                              .withStateType(StateType.PHASE.name())
                              .withAppId(appId)
                              .withDisplayName(generateUuid())
                              .build());
  }

  /**
   * Test to verify fetch Log Records based.
   */
  @Test
  @Repeat(times = 3, successes = 1)
  public void testGetLogRecordsWithQuery() {
    SplunkSetupTestNodeData setupTestNodeData = getSplunkSetupTestNodedata("*exception*");
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  private SplunkSetupTestNodeData getSplunkSetupTestNodedata(String query) {
    return SplunkSetupTestNodeData.builder()
        .query(query)
        .hostNameField("host")
        .appId(appId)
        .settingId(elkSettingId)
        .instanceName("testHost")
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
        .hostExpression("${host.hostName}")
        .workflowId(workflowId)
        .build();
  }
}
