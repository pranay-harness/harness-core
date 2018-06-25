package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.data.structure.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.dl.PageResponse;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 6/14/18.
 */
public class ThirdPartyApiCallLogsIntegrationTest extends BaseIntegrationTest {
  private String stateExecutionId;
  private String appId;
  private String delegateId;
  private String delegateTaskId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    stateExecutionId = generateUuid();
    appId = wingsPersistence.save(anApplication().withName(generateUuid()).withAccountId(accountId).build());
    delegateId = generateUuid();
    delegateTaskId = generateUuid();
  }

  @Test
  public void testSaveApiCallLogs() throws Exception {
    int numOfApiCallLogs = 12;
    List<ThirdPartyApiCallLog> apiCallLogs = new ArrayList<>();
    for (int i = 0; i < numOfApiCallLogs; i++) {
      apiCallLogs.add(ThirdPartyApiCallLog.builder()
                          .appId(appId)
                          .accountId(accountId)
                          .stateExecutionId(stateExecutionId)
                          .requestTimeStamp(i)
                          .request("request-" + i)
                          .response("response-" + i)
                          .delegateId(delegateId)
                          .delegateTaskId(delegateTaskId)
                          .build());
    }
    WebTarget target = client.target(API_BASE + "/delegates/" + delegateTaskId + "/state-executions"
        + "?accountId=" + accountId);
    RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
        entity(apiCallLogs, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertTrue(restResponse.getResource());
    Thread.sleep(2000);

    target = client.target(
        API_BASE + "/activities/" + stateExecutionId + "/api-call-logs?accountId=" + accountId + "&appId=" + appId);
    RestResponse<PageResponse<ThirdPartyApiCallLog>> logResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<PageResponse<ThirdPartyApiCallLog>>>() {});

    List<ThirdPartyApiCallLog> savedApiCallLogs = logResponse.getResource().getResponse();
    assertEquals(numOfApiCallLogs, savedApiCallLogs.size());
    savedApiCallLogs.forEach(savedApiCallLog -> {
      assertNotNull(savedApiCallLog.getUuid());
      savedApiCallLog.setUuid(null);
    });

    assertTrue(CollectionUtils.isEqualCollection(apiCallLogs, savedApiCallLogs));
  }
}
