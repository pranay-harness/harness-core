package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.ElkConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.elk.ElkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ElkResourceIntegrationTest extends BaseIntegrationTest {
  private String elkSettingId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
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
  public void validateQuery() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*)");

    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
  }

  @Test
  public void validateJSONQuery() throws UnsupportedEncodingException {
    String query = "{\"regexp\":{\"log\":{\"value\":\"info\"}}}";
    String encodeQuery = URLEncoder.encode(query, "utf-8");
    WebTarget getTarget =
        client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL + LogAnalysisResource.VALIDATE_QUERY
            + "?accountId=" + accountId + "&query=" + encodeQuery + "&formattedQuery=true");

    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
  }

  @Test
  public void validateQueryFail() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*))");

    try {
      getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
      fail();
    } catch (BadRequestException e) {
      // do nothing
    }
  }

  @Test
  @Ignore
  public void queryHostData() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ANALYSIS_STATE_GET_HOST_RECORD_URL + "?accountId=" + accountId
        + "&serverConfigId=" + elkSettingId
        + "&index=logstash-*&hostNameField=kubernetes.pod_name&hostName=harness-learning-engine&queryType=MATCH"
        + "&query=info&timeStampField=@timestamp&timeStampFieldFormat=yyyy-MM-dd'T'HH:mm:ssXXX&messageField=log");
    RestResponse<Object> response =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Object>>() {});
    List<LogElement> logElements = parseElkResponse(response.getResource(), "info", "@timestamp",
        "yyyy-MM-dd'T'HH:mm:ssXXX", "kubernetes.pod_name", "harness-learning-engine", "log", 0, false);
    assertFalse(logElements.isEmpty());
  }

  @Test
  public void testGetLogRecordsWithNormalQuery() {
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata("error", false);
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  @Test
  public void testGetLogRecordsWithFormattedQuery() {
    String query = "{\"bool\":{\"must\":[{\"query_string\":{\"query\":\"log:error\",\"analyze_wildcard\":true,"
        + "\"default_field\":\"*\"}},{\"range\":{\"@timestamp\":{\"gte\":1535049542943,\"lte\":1535050442943,"
        + "\"format\":\"epoch_millis\"}}}],\"filter\":[],\"should\":[],\"must_not\":[]}}";
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata(query, true);
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));
    String responseString = restResponse.readEntity(String.class);
    JSONObject jsonResponseObject = new JSONObject(responseString);

    JSONObject response = jsonResponseObject.getJSONObject("resource");
    assertEquals("Request failed", restResponse.getStatus(), HttpStatus.SC_OK);
    assertTrue("provider is not reachable", Boolean.valueOf(response.get("providerReachable").toString()));
  }

  @Test
  public void testGetLogRecordsWithInvalidFormattedQuery() {
    // doesnt start with '{' its an invalid query String
    String query = "must\":[{\"query_string\":{\"query\":\"log:error\",\"analyze_wildcard\":true,"
        + "\"default_field\":\"*\"}},{\"range\":{\"@timestamp\":{\"gte\":1535049542943,\"lte\":1535050442943,"
        + "\"format\":\"epoch_millis\"}}}],\"filter\":[],\"should\":[],\"must_not\":[]}";
    ElkSetupTestNodeData elkSetupTestNodeData = getElkSetupTestNodedata(query, true);
    WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.TEST_NODE_DATA + "?accountId=" + accountId);
    Response restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(elkSetupTestNodeData, MediaType.APPLICATION_JSON));

    assertEquals(restResponse.getStatus(), HttpStatus.SC_BAD_REQUEST);
  }

  private ElkSetupTestNodeData getElkSetupTestNodedata(String query, Boolean formattedQuery) {
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
        .formattedQuery(formattedQuery)
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
}
