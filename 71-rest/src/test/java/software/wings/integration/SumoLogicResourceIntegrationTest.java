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

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.HostElement;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SumoConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.sumo.SumoLogicSetupTestNodedata;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 08/27/2018
 */
public class SumoLogicResourceIntegrationTest extends BaseIntegrationTest {
  private String sumoSettingId;
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
    sumoSettingId = wingsPersistence.save(
        Builder.aSettingAttribute()
            .withName(generateUuid())
            .withAccountId(accountId)
            .withValue(SumoConfig.builder()
                           .sumoUrl("https://api.us2.sumologic.com/api/v1/")
                           .accessId(scmSecret.decryptToCharArray(new SecretName("sumo_config_access_id")))
                           .accessKey(scmSecret.decryptToCharArray(new SecretName("sumo_config_access_key")))
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
  @Owner(developers = PRANJAL, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetSampleLogRecordVerifyCall() {
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SUMO_RESOURCE_BASE_URL
        + LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL + "?accountId=" + accountId
        + "&serverConfigId=" + sumoSettingId + "&durationInMinutes=" + 24 * 60);

    Response restResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<Response>() {});

    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetLogRecords() {
    SumoLogicSetupTestNodedata testNodedata = getSumoLogicSampledata();
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SUMO_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId + "&serverConfigId=" + sumoSettingId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(testNodedata, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  private SumoLogicSetupTestNodedata getSumoLogicSampledata() {
    return SumoLogicSetupTestNodedata.builder()
        .query("*exception*")
        .hostNameField("_sourceHost")
        .appId(appId)
        .settingId(sumoSettingId)
        .instanceName("testHost")
        .instanceElement(
            anInstanceElement()
                .uuid("8cec1e1b0d16")
                .displayName("8cec1e1b0d16")
                .hostName("testHost")
                .dockerId("8cec1e1b0d16")
                .host(HostElement.builder()
                          .uuid("8cec1e1b0d16")
                          .hostName("ip-172-31-28-247")
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
}
