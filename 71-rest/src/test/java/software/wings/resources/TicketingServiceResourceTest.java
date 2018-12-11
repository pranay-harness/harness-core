package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.JiraHelperService.APPROVAL_FIELD_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_ID_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_VALUE_KEY;
import static software.wings.service.impl.JiraHelperService.APP_ID_KEY;
import static software.wings.service.impl.JiraHelperService.WORKFLOW_EXECUTION_ID_KEY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Matchers;
import software.wings.WingsBaseTest;
import software.wings.api.JiraExecutionData;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.ResourceTestRule;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class TicketingServiceResourceTest extends WingsBaseTest {
  private static final WorkflowExecutionService WORKFLOW_EXECUTION_SERVICE = mock(WorkflowExecutionService.class);
  private static final JiraHelperService JIRA_HELPER_SERVICE = mock(JiraHelperService.class);
  private static final WaitNotifyEngine WAIT_NOTIFY_ENGINE = mock(WaitNotifyEngine.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new TicketingServiceResource(JIRA_HELPER_SERVICE,
                                                           WORKFLOW_EXECUTION_SERVICE, WAIT_NOTIFY_ENGINE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();
  @Test
  public void testPingJiraUpdates() throws UnsupportedEncodingException {
    Map<String, String> claimsMap = getClaimsMap();
    String token = generateMockJWTToken(claimsMap);
    when(JIRA_HELPER_SERVICE.validateJiraToken(Matchers.anyString()))
        .then(invocationOnMock -> getClaimMockMap(claimsMap));
    when(WORKFLOW_EXECUTION_SERVICE.fetchJiraExecutionDataFromWorkflowExecution(
             Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.any()))
        .thenReturn(mock(JiraExecutionData.class));

    String json =
        "{\"timestamp\": 1543331334925,\"webhookEvent\": \"jira:issue_updated\",\"issue_event_type_name\": \"issue_generic\",\"user\": {\"self\": \"http://localhost:8080/rest/api/2/user?username=pooja.singhal\",\"name\": \"pooja.singhal\",\"key\": \"pooja.singhal\",\"emailAddress\": \"pooja.singhal@harness.io\",\"avatarUrls\": {\"48x48\": \"http://localhost:8080/secure/useravatar?avatarId=10337\",\"24x24\": \"http://localhost:8080/secure/useravatar?size=small&avatarId=10337\",\"16x16\": \"http://localhost:8080/secure/useravatar?size=xsmall&avatarId=10337\",\"32x32\": \"http://localhost:8080/secure/useravatar?size=medium&avatarId=10337\"},\"displayName\": \"pooja.singhal@harness.io\",\"active\": true,\"timeZone\": \"Asia/Kolkata\"},\"changelog\": {\"id\": \"10203\",\"items\": [{\"field\": \"Status\",\"fieldtype\": \"jira\",\"from\": \"10001\",\"fromString\": \"Done\",\"to\": \"10000\",\"toString\": \"TO DO\"}]}}";
    RestResponse<Boolean> restResponse =
        RESOURCES.client()
            .target(format("/ticketing/jira-approval/") + token)
            .request()
            .post(entity(json, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", false);

    json =
        "{\"timestamp\": 1543331334925,\"webhookEvent\": \"jira:issue_updated\",\"issue_event_type_name\": \"issue_generic\",\"user\": {\"self\": \"http://localhost:8080/rest/api/2/user?username=pooja.singhal\",\"name\": \"pooja.singhal\",\"key\": \"pooja.singhal\",\"emailAddress\": \"pooja.singhal@harness.io\",\"avatarUrls\": {\"48x48\": \"http://localhost:8080/secure/useravatar?avatarId=10337\",\"24x24\": \"http://localhost:8080/secure/useravatar?size=small&avatarId=10337\",\"16x16\": \"http://localhost:8080/secure/useravatar?size=xsmall&avatarId=10337\",\"32x32\": \"http://localhost:8080/secure/useravatar?size=medium&avatarId=10337\"},\"displayName\": \"pooja.singhal@harness.io\",\"active\": true,\"timeZone\": \"Asia/Kolkata\"},\"changelog\": {\"id\": \"10203\",\"items\": [{\"field\": \"Status\",\"fieldtype\": \"jira\",\"from\": \"10001\",\"fromString\": \"Done\",\"to\": \"10000\",\"toString\": \"APPROVED\"}]}}";

    restResponse = RESOURCES.client()
                       .target(format("/ticketing/jira-approval/") + token)
                       .request()
                       .post(entity(json, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", true);
  }

  private String generateMockJWTToken(Map<String, String> claims) throws UnsupportedEncodingException {
    String jwtPasswordSecret = "test_secret";
    Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
    Builder jwtBuilder = JWT.create().withIssuer("Harness Inc").withIssuedAt(new Date());
    if (!isEmpty(claims)) {
      claims.forEach(jwtBuilder::withClaim);
    }
    return jwtBuilder.sign(algorithm);
  }

  private Map<String, String> getClaimsMap() {
    Map<String, String> claims = new HashMap<>();
    claims.put(APPROVAL_ID_KEY, generateUuid());
    claims.put(APPROVAL_FIELD_KEY, "status");
    claims.put(APPROVAL_VALUE_KEY, "approved");
    claims.put(APP_ID_KEY, APP_ID);
    claims.put(WORKFLOW_EXECUTION_ID_KEY, PIPELINE_WORKFLOW_EXECUTION_ID);
    return claims;
  }

  private Map<String, Claim> getClaimMockMap(Map<String, String> values) {
    Map<String, Claim> newMap = new HashMap<>();
    for (Entry<String, String> entry : values.entrySet()) {
      Claim claim = mock(Claim.class);
      when(claim.asString()).thenReturn(entry.getValue());
      when(claim.isNull()).thenReturn(false);
      newMap.put(entry.getKey(), claim);
    }
    return newMap;
  }
}