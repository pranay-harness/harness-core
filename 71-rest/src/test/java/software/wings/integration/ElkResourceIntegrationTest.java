package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.elk.ElkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ElkResourceIntegrationTest extends BaseIntegrationTest {
  private String elkSettingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    elkSettingId =
        wingsPersistence.save(Builder.aSettingAttribute()
                                  .withName(generateUuid())
                                  .withAccountId(accountId)
                                  .withValue(ElkConfig.builder()
                                                 .elkConnector(ElkConnector.ELASTIC_SEARCH_SERVER)
                                                 .elkUrl("http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/")
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
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void queryHostData() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ANALYSIS_STATE_GET_HOST_RECORD_URL + "?accountId=" + accountId
        + "&serverConfigId=" + elkSettingId
        + "&index=logstash-*&hostNameField=kubernetes.pod_name&hostName=harness-learning-engine&queryType=MATCH"
        + "&query=info&timeStampField=@timestamp&timeStampFieldFormat=yyyy-MM-dd'T'HH:mm:ssXXX&messageField=log");
    RestResponse<Object> response =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Object>>() {});
    List<LogElement> logElements = parseElkResponse(response.getResource(), "info", "@timestamp",
        "yyyy-MM-dd'T'HH:mm:ssXXX", "kubernetes.pod_name", "harness-learning-engine", "log", 0, false, -1, -1);
    assertThat(logElements.isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Category(IntegrationTests.class)
  public void testGetLogRecordsWithNormalQuery() {
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata("error");
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(IntegrationTests.class)
  public void testGetLogRecordsWithValidJSONQuery() {
    String query = "{\"bool\":{\"must\":[{\"query_string\":{\"query\":\"log:error\",\"analyze_wildcard\":true,"
        + "\"default_field\":\"*\"}},{\"range\":{\"@timestamp\":{\"gte\":1535049542943,\"lte\":1535050442943,"
        + "\"format\":\"epoch_millis\"}}}],\"filter\":[],\"should\":[],\"must_not\":[]}}";
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata(query);
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertThat(HttpStatus.SC_OK).isEqualTo(restResponse.getStatus());
    assertThat(Boolean.valueOf(response.get("providerReachable").toString())).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Category(IntegrationTests.class)
  public void testGetLogRecordsWithInvalidJSONQuery() {
    // doesnt start with '{' its an invalid query String
    String query = "must\":[{\"query_string\":{\"query\":\"log:error\",\"analyze_wildcard\":true,"
        + "\"default_field\":\"*\"}},{\"range\":{\"@timestamp\":{\"gte\":1535049542943,\"lte\":1535050442943,"
        + "\"format\":\"epoch_millis\"}}}],\"filter\":[],\"should\":[],\"must_not\":[]}";
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata(query);
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));

    assertThat(HttpStatus.SC_BAD_REQUEST).isEqualTo(restResponse.getStatus());
  }

  private ElkSetupTestNodeData getElkSetupTestNodedata(String query) {
    return ElkSetupTestNodeData.builder()
        .query(query)
        .indices("logstash-*")
        .messageField("log")
        .timeStampField("@timestamp")
        .timeStampFieldFormat("yyyy-MM-dd'T'HH:mm:ssz")
        .queryType(ElkQueryType.TERM)
        .hostNameField("kubernetes.host")
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
                .serviceTemplateElement(aServiceTemplateElement().withUuid("8cec1e1b0d16").withName(null).build())
                .podName("testHost")
                .workloadName("testHost")
                .build())
        .workflowId(workflowId)
        .build();
  }
}
