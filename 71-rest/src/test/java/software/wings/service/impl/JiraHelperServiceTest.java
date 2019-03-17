package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.JiraHelperService.APPROVAL_FIELD_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_ID_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_VALUE_KEY;
import static software.wings.service.impl.JiraHelperService.APP_ID_KEY;
import static software.wings.service.impl.JiraHelperService.REJECTION_FIELD_KEY;
import static software.wings.service.impl.JiraHelperService.REJECTION_VALUE_KEY;
import static software.wings.service.impl.JiraHelperService.WORKFLOW_EXECUTION_ID_KEY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class JiraHelperServiceTest extends WingsBaseTest {
  @Mock private DelegateServiceImpl delegateService;
  @Mock private transient SecretManager secretManager;
  @Mock SettingsService settingService;
  @Mock WorkflowExecutionService workflowExecutionService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock software.wings.security.SecretManager secretManagerForToken;

  @InjectMocks JiraHelperService jiraHelperService = new JiraHelperService();
  private String token;

  @Test
  @Category(UnitTests.class)
  public void checkApprovalFromWebhookCallback() throws UnsupportedEncodingException {
    Map<String, String> claimsMap = getClaimsMap();
    token = generateMockJWTToken(claimsMap);
    when(secretManagerForToken.verifyJWTToken(Matchers.anyString(), Matchers.any()))
        .then(invocationOnMock -> getClaimMockMap(claimsMap));
    when(workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
             Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.any()))
        .thenReturn(mock(ApprovalStateExecutionData.class));
    String json =
        "{\"timestamp\": 1543331334925,\"webhookEvent\": \"jira:issue_updated\",\"issue_event_type_name\": \"issue_generic\",\"user\": {\"self\": \"http://localhost:8080/rest/api/2/user?username=pooja.singhal\",\"name\": \"pooja.singhal\",\"key\": \"pooja.singhal\",\"emailAddress\": \"pooja.singhal@harness.io\",\"avatarUrls\": {\"48x48\": \"http://localhost:8080/secure/useravatar?avatarId=10337\",\"24x24\": \"http://localhost:8080/secure/useravatar?size=small&avatarId=10337\",\"16x16\": \"http://localhost:8080/secure/useravatar?size=xsmall&avatarId=10337\",\"32x32\": \"http://localhost:8080/secure/useravatar?size=medium&avatarId=10337\"},\"displayName\": \"pooja.singhal@harness.io\",\"active\": true,\"timeZone\": \"Asia/Kolkata\"},\"changelog\": {\"id\": \"10203\",\"items\": [{\"field\": \"Status\",\"fieldtype\": \"jira\",\"from\": \"10001\",\"fromString\": \"Done\",\"to\": \"10000\",\"toString\": \"rejected\"}]}}";

    ExecutionStatus approvalStatus = jiraHelperService.checkApprovalFromWebhookCallback(token, json);
    assertEquals(approvalStatus, ExecutionStatus.REJECTED);
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
    claims.put(REJECTION_FIELD_KEY, "status");
    claims.put(REJECTION_VALUE_KEY, "rejected");
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

  @Test
  @Category(UnitTests.class)
  public void getApprovalStatusTest() {}
}