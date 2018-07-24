package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.ElkConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class ElkResourceIntegrationTest extends BaseIntegrationTest {
  private String elkSettingId;
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
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
  }

  @Test
  public void validateQuery() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ELK_VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*)");

    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
  }

  @Test
  public void validateQueryFail() {
    WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
        + LogAnalysisResource.ELK_VALIDATE_QUERY + "?accountId=" + accountId + "&query=(.*exception.*))");

    try {
      getRequestBuilderWithAuthHeader(getTarget).get(new GenericType<RestResponse<Boolean>>() {});
      fail();
    } catch (BadRequestException e) {
      // do nothing
    }
  }

  @Test
  @Ignore
  public void queryHostData() throws Exception {
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
}
