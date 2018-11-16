package software.wings.service.impl.trigger;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_BRANCH_REF;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.GH_PUSH_HEAD_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.GH_PUSH_REF_BRANCH;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PUSH_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PUSH_REF_BRANCH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;
import software.wings.expression.ManagerExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

@Singleton
public class WebhookEventUtils {
  public static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  public static final String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  public static final String X_BIT_BUCKET_EVENT = "X-Event-Key";

  @Inject private ManagerExpressionEvaluator expressionEvaluator;

  public static final List<String> eventHeaders =
      Collections.unmodifiableList(asList(X_GIT_HUB_EVENT, X_GIT_LAB_EVENT, X_BIT_BUCKET_EVENT));

  public WebhookSource obtainWebhookSource(HttpHeaders httpHeaders) {
    if (httpHeaders == null) {
      throw new InvalidRequestException("Failed to resolve Webhook Source. Reason: HttpHeaders are empty.");
    }
    if (httpHeaders.getHeaderString(X_GIT_HUB_EVENT) != null) {
      return WebhookSource.GITHUB;
    } else if (httpHeaders.getHeaderString(X_GIT_LAB_EVENT) != null) {
      return WebhookSource.GITLAB;
    } else if (httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT) != null) {
      return WebhookSource.BITBUCKET;
    }
    throw new InvalidRequestException("Unable to resolve the Webhook Source. "
        + "One of the " + eventHeaders + " must be present in Headers");
  }

  public String obtainBranchName(WebhookSource webhookSource, HttpHeaders httpHeaders, Map<String, Object> payload) {
    switch (webhookSource) {
      case GITHUB: {
        switch (GitHubEventType.find(httpHeaders.getHeaderString(X_GIT_HUB_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(GH_PUSH_REF_BRANCH, payload);
          default:
            return null;
        }
      }
      case GITLAB: {
        switch (GitLabEventType.find(httpHeaders.getHeaderString(X_GIT_LAB_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(GIT_LAB_PUSH_REF_BRANCH, payload);
          default:
            return null;
        }
      }
      case BITBUCKET:
        switch (BitBucketEventType.find(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(BIT_BUCKET_BRANCH_REF, payload);
          default:
            return null;
        }
      default:
        unhandled(webhookSource);
    }

    throw new InvalidRequestException("Failed to obtain branch from the payload");
  }

  public String obtainCommitId(WebhookSource webhookSource, HttpHeaders httpHeaders, Map<String, Object> payload) {
    switch (webhookSource) {
      case GITHUB: {
        switch (GitHubEventType.find(httpHeaders.getHeaderString(X_GIT_HUB_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(GH_PUSH_HEAD_COMMIT_ID, payload);
          default:
            return null;
        }
      }
      case GITLAB:
        switch (GitLabEventType.find(httpHeaders.getHeaderString(X_GIT_LAB_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(GIT_LAB_PUSH_COMMIT_ID, payload);
          default:
            return null;
        }
      case BITBUCKET:
        switch (BitBucketEventType.find(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT))) {
          case PUSH:
            return expressionEvaluator.substitute(BIT_BUCKET_COMMIT_ID, payload);
          default:
            return null;
        }
      default:
        unhandled(webhookSource);
        return null;
    }
  }

  public String obtainEventType(WebhookSource webhookSource, HttpHeaders httpHeaders) {
    switch (webhookSource) {
      case GITHUB:
        return GitHubEventType.find(httpHeaders.getHeaderString(X_GIT_HUB_EVENT)).getValue();
      case GITLAB:
        return GitLabEventType.find(httpHeaders.getHeaderString(X_GIT_LAB_EVENT)).getValue();
      case BITBUCKET:
        return BitBucketEventType.find(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT)).getValue();
      default:
        unhandled(webhookSource);
        return null;
    }
  }

  public String obtainPrAction(WebhookSource webhookSource, Map<String, Object> payload) {
    switch (webhookSource) {
      case GITHUB:
        return payload.get("action") == null ? null : payload.get("action").toString();
      default:
        return null;
    }
  }
}
