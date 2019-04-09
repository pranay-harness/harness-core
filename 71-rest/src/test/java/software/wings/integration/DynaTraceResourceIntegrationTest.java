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
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Pranjal on 09/13/2018
 */
@Ignore
public class DynaTraceResourceIntegrationTest extends BaseIntegrationTest {
  public static final String url = "https://bdv73347.live.dynatrace.com";
  @Inject private ScmSecret scmSecret;

  private String settingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
    settingId = wingsPersistence.save(
        Builder.aSettingAttribute()
            .withName(generateUuid())
            .withAccountId(accountId)
            .withValue(DynaTraceConfig.builder()
                           .dynaTraceUrl(url)
                           .apiToken(scmSecret.decryptToCharArray(new SecretName("dyna_trace_api_key")))
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
  @Owner(emails = "pranjal@harness.io", intermittent = true)
  @Repeat(times = TIMES_TO_REPEAT, successes = SUCCESS_COUNT)
  @Category(IntegrationTests.class)
  public void testGetLogRecords() {
    DynaTraceSetupTestNodeData testNodedata = getSampledata();
    WebTarget target = client.target(API_BASE + "/"
        + "dynatrace" + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(testNodedata, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  private DynaTraceSetupTestNodeData getSampledata() {
    long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    long fromTime = toTime - TimeUnit.MINUTES.toMillis(15) / TimeUnit.SECONDS.toMillis(1);
    Set<String> serviceMethods = new HashSet<>();
    serviceMethods.add("SERVICE_METHOD-991CE862F114C79F");
    serviceMethods.add("SERVICE_METHOD-65C2EED098275731");
    serviceMethods.add("SERVICE_METHOD-9D3499F155C8070D");
    serviceMethods.add("SERVICE_METHOD-AECEC4A5C7E348EC");
    serviceMethods.add("SERVICE_METHOD-9ACB771237BE05C6");
    serviceMethods.add("SERVICE_METHOD-DA487A489220E53D");
    return DynaTraceSetupTestNodeData.builder()
        .serviceMethods(serviceMethods)
        .appId(appId)
        .settingId(settingId)
        .instanceName("testHost")
        .instanceElement(
            anInstanceElement()
                .withUuid("8cec1e1b0d16")
                .withDisplayName("8cec1e1b0d16")
                .withHostName("testHost")
                .withDockerId("8cec1e1b0d16")
                .withHost(aHostElement()
                              .withUuid("8cec1e1b0d16")
                              .withHostName("ip-172-31-28-247")
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
        .fromTime(fromTime)
        .toTime(toTime)
        .build();
  }
}
