package software.wings.service.impl.slack;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.security.SecretManager.JWT_CATEGORY.EXTERNAL_SERVICE_SECRET;
import static software.wings.sm.states.ApprovalState.JSON;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.text.StrSubstitutor;
import org.json.JSONObject;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.approval.SlackApprovalParams;
import software.wings.security.SecretManager;
import software.wings.service.impl.notifications.SlackApprovalMessageKeys;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class SlackApprovalUtils {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ConfirmationHandler confirmationHandler;
  @Inject private ProceedResponseHandler proceedResponseHandler;
  @Inject private RevertResponseHandler revertResponseHandler;
  @Inject private SecretManager secretManager;

  public RestResponse<Boolean> slackApprovalHandler(MultivaluedMap<String, String> body) throws IOException {
    logger.info("SLACK REQUEST");

    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> slackUserDetails =
        (Map<String, String>) mapper.readValue(body.getFirst("payload"), Map.class).get("user");
    Map<String, String> action =
        ((ArrayList<Map>) mapper.readValue(body.getFirst("payload"), Map.class).get("actions")).get(0);

    // Slack Details
    String actionType = action.get("action_id");
    String responseUrl = (String) mapper.readValue(body.getFirst("payload"), Map.class).get("response_url");
    SlackApprovalParams slackApprovalParams = mapper.readValue(action.get("value"), SlackApprovalParams.class);
    final SlackApprovalParams approvalParams = slackApprovalParams.toBuilder()
                                                   .actionType(actionType)
                                                   .slackUsername(slackUserDetails.get("name"))
                                                   .slackUserId(slackUserDetails.get("id"))
                                                   .build();

    URL sessionTimeoutTemplateUrl =
        this.getClass().getResource(SlackApprovalMessageKeys.APPROVAL_EXPIRED_MESSAGE_TEMPLATE);
    Map<String, String> sessionTimeoutTemplateFillers = new HashMap<>();
    sessionTimeoutTemplateFillers.put(
        SlackApprovalMessageKeys.WORKFLOW_EXECUTION_NAME, approvalParams.getWorkflowExecutionName());
    sessionTimeoutTemplateFillers.put(SlackApprovalMessageKeys.APP_NAME, approvalParams.getAppName());
    String sessionTimedOutMessage = createMessageFromTemplate(sessionTimeoutTemplateUrl, sessionTimeoutTemplateFillers);

    // Creating original slack notification message
    URL notificationTemplateUrl;
    if (approvalParams.isPipeline()) {
      notificationTemplateUrl =
          this.getClass().getResource(SlackApprovalMessageKeys.PIPELINE_APPROVAL_MESSAGE_TEMPLATE);
    } else {
      notificationTemplateUrl =
          this.getClass().getResource(SlackApprovalMessageKeys.WORKFLOW_APPROVAL_MESSAGE_TEMPLATE);
    }
    String slackNotificationMessage = createSlackApprovalMessage(approvalParams, notificationTemplateUrl);

    if (!approvalParams.isConfirmation()) {
      return confirmationHandler.handle(approvalParams, slackNotificationMessage, sessionTimedOutMessage, responseUrl);
    } else {
      // Confirmation Response : Proceed
      if (actionType.equals(SlackApprovalMessageKeys.BUTTON_PROCEED)) {
        return proceedResponseHandler.handle(
            approvalParams, slackNotificationMessage, sessionTimedOutMessage, responseUrl);
      } else {
        // Confirmation Response : Revert
        return revertResponseHandler.handle(
            approvalParams, slackNotificationMessage, sessionTimedOutMessage, responseUrl);
      }
    }
  }

  public boolean verifyJwtToken(SlackApprovalParams slackApprovalParams) throws UnsupportedEncodingException {
    Algorithm algorithm = Algorithm.HMAC256(secretManager.getJWTSecret(EXTERNAL_SERVICE_SECRET));
    JWTVerifier verifier = JWT.require(algorithm)
                               .withIssuer("Harness Inc")
                               .withClaim("approvalId", slackApprovalParams.getApprovalId())
                               .build();
    try {
      verifier.verify(slackApprovalParams.getJwtToken());
      return true;
    } catch (JWTVerificationException e) {
      return false;
    }
  }

  public static String createSlackApprovalMessage(SlackApprovalParams slackApprovalParams, URL url) {
    Map<String, String> templateFillers = new HashMap<>();
    templateFillers.put(SlackApprovalMessageKeys.PAUSED_STAGE_NAME, slackApprovalParams.getPausedStageName());
    templateFillers.put(SlackApprovalMessageKeys.WORKFLOW_URL, slackApprovalParams.getWorkflowUrl());
    templateFillers.put(
        SlackApprovalMessageKeys.WORKFLOW_EXECUTION_NAME, slackApprovalParams.getWorkflowExecutionName());
    templateFillers.put(SlackApprovalMessageKeys.APP_NAME, slackApprovalParams.getAppName());
    templateFillers.put(SlackApprovalMessageKeys.SERVICES, slackApprovalParams.getServicesInvolved());
    templateFillers.put(SlackApprovalMessageKeys.ENVIRONMENTS, slackApprovalParams.getEnvironmentsInvolved());
    templateFillers.put(SlackApprovalMessageKeys.ARTIFACTS, slackApprovalParams.getArtifactsInvolved());

    return createMessageFromTemplate(url, templateFillers);
  }

  public static RequestBody createBody(String bodyText, boolean replaceOriginal) {
    JSONObject jsonPayload = new JSONObject();
    if (replaceOriginal) {
      jsonPayload.put("replace_original", "true");
    }
    jsonPayload.put("text", bodyText);
    String payload = jsonPayload.toString();
    return RequestBody.create(JSON, payload);
  }

  public static String createMessageFromTemplate(URL templateUrl, Map<String, String> templateFillers) {
    String loadedTemplate = "";
    StrSubstitutor sub = new StrSubstitutor(templateFillers);
    try {
      loadedTemplate = Resources.toString(templateUrl, Charsets.UTF_8);
    } catch (IOException e) {
      logger.error("Error in loading given template");
    }
    return sub.replace(loadedTemplate);
  }

  public static RestResponse<Boolean> slackPostRequest(RequestBody body, String responseUrl) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request1 = new Request.Builder()
                           .url(responseUrl)
                           .post(body)
                           .addHeader("Content-Type", "application/json")
                           .addHeader("Accept", "*/*")
                           .addHeader("Cache-Control", "no-cache")
                           .addHeader("Host", "hooks.slack.com")
                           .addHeader("accept-encoding", "gzip, deflate")
                           .addHeader("content-length", "798")
                           .addHeader("Connection", "keep-alive")
                           .addHeader("cache-control", "no-cache")
                           .build();

    client.newCall(request1).execute();
    return new RestResponse<>(true);
  }

  public static boolean approve(String appId, String workflowExecutionId, String stateExecutionId,
      ApprovalDetails approvalDetails, WorkflowExecutionService workflowExecutionService) {
    ApprovalStateExecutionData approvalStateExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            appId, workflowExecutionId, stateExecutionId, approvalDetails);

    return workflowExecutionService.approveOrRejectExecution(
        appId, approvalStateExecutionData.getUserGroups(), approvalDetails);
  }
}