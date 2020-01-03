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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.ServiceElement;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SplunkConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.splunk.SplunkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 09/01/2018
 */
public class SplunkResourceIntegrationTest extends BaseIntegrationTest {
  private static final String SPLUNK_CLOUD_URL = "https://api-prd-p-429h4vj2lsng.cloud.splunk.com:8089";
  private String elkSettingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;

  @Inject private ScmSecret scmSecret;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    elkSettingId = wingsPersistence.save(
        Builder.aSettingAttribute()
            .withName(generateUuid())
            .withAccountId(accountId)
            .withValue(SplunkConfig.builder()
                           .accountId(accountId)
                           .splunkUrl(SPLUNK_CLOUD_URL)
                           .username(scmSecret.decryptToString(new SecretName("splunk_cloud_username")))
                           .password(scmSecret.decryptToString(new SecretName("splunk_cloud_password")).toCharArray())
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

  /**
   * Test to verify fetch Log Records based.
   */
  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testGetLogRecordsWithQuery() {
    SplunkSetupTestNodeData setupTestNodeData = getSplunkSetupTestNodedata("*exception*");
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(setupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  private SplunkSetupTestNodeData getSplunkSetupTestNodedata(String query) {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    long fromTime = toTime - TimeUnit.MINUTES.toMillis(20) / TimeUnit.SECONDS.toMillis(1);
    return SplunkSetupTestNodeData.builder()
        .query(query)
        .hostNameField("host")
        .appId(appId)
        .settingId(elkSettingId)
        .instanceName("testHost")
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
                .serviceTemplateElement(
                    aServiceTemplateElement()
                        .withUuid("8cec1e1b0d16")
                        .withName(null)
                        .withServiceElement(ServiceElement.builder().uuid(generateUuid()).name(generateUuid()).build())
                        .build())
                .podName("testHost")
                .workloadName("testHost")
                .build())
        .hostExpression("${host.hostName}")
        .toTime(toTime)
        .fromTime(fromTime)
        .workflowId(workflowId)
        .build();
  }
}
