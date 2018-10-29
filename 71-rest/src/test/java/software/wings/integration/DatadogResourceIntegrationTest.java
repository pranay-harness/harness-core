package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.DatadogConfig;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.service.impl.datadog.DataDogFetchConfig;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

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
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
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

  @Test
  public void testGetLogRecordsWithNormalQuery() {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    DataDogFetchConfig fetchConfig =
        DataDogFetchConfig.builder()
            .metrics("docker.cpu.usage,docker.mem.rss")
            .datadogServiceName("test")
            .hostName("harness-verification.todolist-datadog.gcp-697-564fd64c87-mj7th")
            .fromtime(toTime - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1))
            .toTime(toTime)
            .build();

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
}