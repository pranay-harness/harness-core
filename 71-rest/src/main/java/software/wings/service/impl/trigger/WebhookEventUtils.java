package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_PULL_BRANCH_REF;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_PUSH_BRANCH_REF;
import static software.wings.beans.trigger.WebhookParameters.BIT_BUCKET_REFS_CHANGED_REF;
import static software.wings.beans.trigger.WebhookParameters.GH_PULL_REF_BRANCH;
import static software.wings.beans.trigger.WebhookParameters.GH_PUSH_HEAD_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.GH_PUSH_REF_BRANCH;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PULL_REF_BRANCH;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PUSH_COMMIT_ID;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PUSH_REF_BRANCH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.PayloadSource.Type;
import software.wings.beans.trigger.WebhookEventType;
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
@Slf4j
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
            + "One of the [" + eventHeaders + "] must be present in Headers",
        USER);
  }

  public String findExpression(Map<String, Object> payLoadMap, String storedExpression) {
    return expressionEvaluator.substitute(storedExpression, payLoadMap);
  }

  public Type obtainEventType(HttpHeaders httpHeaders) {
    if (httpHeaders == null) {
      throw new InvalidRequestException("Failed to resolve Webhook Source. Reason: HttpHeaders are empty.");
    }
    if (httpHeaders.getHeaderString(X_GIT_HUB_EVENT) != null) {
      return Type.GITHUB;
    } else if (httpHeaders.getHeaderString(X_GIT_LAB_EVENT) != null) {
      return Type.GITLAB;
    } else if (httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT) != null) {
      return Type.BITBUCKET;
    }
    throw new InvalidRequestException("Unable to resolve the Webhook Source. "
            + "One of the " + eventHeaders + " must be present in Headers",
        USER);
  }

  public String obtainBranchName(WebhookSource webhookSource, HttpHeaders httpHeaders, Map<String, Object> payload) {
    try {
      switch (webhookSource) {
        case GITHUB: {
          GitHubEventType gitHubEventType = getGitHubEventType(httpHeaders);
          if (gitHubEventType == null) {
            return null;
          }
          switch (gitHubEventType) {
            case PUSH:
              return expressionEvaluator.substitute(GH_PUSH_REF_BRANCH, payload);
            case PULL_REQUEST:
              return expressionEvaluator.substitute(GH_PULL_REF_BRANCH, payload);
            default:
              return null;
          }
        }
        case GITLAB: {
          GitLabEventType gitLabEventType = getGitLabEventType(httpHeaders);
          if (gitLabEventType == null) {
            return null;
          }
          switch (gitLabEventType) {
            case PUSH:
              return expressionEvaluator.substitute(GIT_LAB_PUSH_REF_BRANCH, payload);
            case PULL_REQUEST:
              return expressionEvaluator.substitute(GIT_LAB_PULL_REF_BRANCH, payload);
            default:
              return null;
          }
        }
        case BITBUCKET:
          BitBucketEventType bitBucketEventType = getBitBucketEventType(httpHeaders);
          if (bitBucketEventType == null) {
            return null;
          }
          switch (bitBucketEventType) {
            case PUSH:
              return expressionEvaluator.substitute(BIT_BUCKET_PUSH_BRANCH_REF, payload);
            case REFS_CHANGED:
              return expressionEvaluator.substitute(BIT_BUCKET_REFS_CHANGED_REF, payload);

            case PULL_REQUEST_CREATED:
            case PULL_REQUEST_UPDATED:
            case PULL_REQUEST_APPROVED:
            case PULL_REQUEST_APPROVAL_REMOVED:
            case PULL_REQUEST_MERGED:
            case PULL_REQUEST_DECLINED:
            case PULL_REQUEST_COMMENT_CREATED:
            case PULL_REQUEST_COMMENT_UPDATED:
            case PULL_REQUEST_COMMENT_DELETED:
              return expressionEvaluator.substitute(BIT_BUCKET_PULL_BRANCH_REF, payload);

            default:
              return null;
          }
        default:
          unhandled(webhookSource);
          return null;
      }
    } catch (Exception e) {
      logger.error("Failed to resolve the branch name from payload {} and headers {}", payload, httpHeaders);
      return null;
    }
  }

  private GitHubEventType getGitHubEventType(HttpHeaders httpHeaders) {
    logger.info("Git Hub Event header {}", httpHeaders.getHeaderString(X_GIT_HUB_EVENT));
    return GitHubEventType.find(httpHeaders.getHeaderString(X_GIT_HUB_EVENT));
  }

  public String obtainCommitId(WebhookSource webhookSource, HttpHeaders httpHeaders, Map<String, Object> payload) {
    try {
      switch (webhookSource) {
        case GITHUB: {
          GitHubEventType gitHubEventType = getGitHubEventType(httpHeaders);
          if (gitHubEventType == null) {
            return null;
          }
          switch (gitHubEventType) {
            case PUSH:
              return expressionEvaluator.substitute(GH_PUSH_HEAD_COMMIT_ID, payload);
            default:
              return null;
          }
        }
        case GITLAB:
          GitLabEventType gitLabEventType = getGitLabEventType(httpHeaders);
          if (gitLabEventType == null) {
            return null;
          }
          switch (gitLabEventType) {
            case PUSH:
              return expressionEvaluator.substitute(GIT_LAB_PUSH_COMMIT_ID, payload);
            default:
              return null;
          }
        case BITBUCKET:
          BitBucketEventType bitBucketEventType = getBitBucketEventType(httpHeaders);
          if (bitBucketEventType == null) {
            return null;
          }
          switch (bitBucketEventType) {
            case PUSH:
              return expressionEvaluator.substitute(BIT_BUCKET_COMMIT_ID, payload);
            default:
              return null;
          }
        default:
          unhandled(webhookSource);
          return null;
      }
    } catch (Exception ex) {
      logger.error("Failed to resolve the branch name from payload {} and headers {}", payload, httpHeaders);
      return null;
    }
  }

  private BitBucketEventType getBitBucketEventType(HttpHeaders httpHeaders) {
    logger.info("Bit Bucket event header {}", httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT));
    return BitBucketEventType.find(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT));
  }

  private GitLabEventType getGitLabEventType(HttpHeaders httpHeaders) {
    logger.info("Git Lab Event header {}", httpHeaders.getHeaderString(X_GIT_LAB_EVENT));
    return GitLabEventType.find(httpHeaders.getHeaderString(X_GIT_LAB_EVENT));
  }

  public String obtainEventType(WebhookSource webhookSource, HttpHeaders httpHeaders) {
    switch (webhookSource) {
      case GITHUB:
        GitHubEventType gitHubEventType = getGitHubEventType(httpHeaders);
        return gitHubEventType == null ? httpHeaders.getHeaderString(X_GIT_HUB_EVENT) : gitHubEventType.getValue();
      case GITLAB:
        GitLabEventType gitLabEventType = getGitLabEventType(httpHeaders);
        return gitLabEventType == null ? httpHeaders.getHeaderString(X_GIT_LAB_EVENT) : gitLabEventType.getValue();
      case BITBUCKET:
        BitBucketEventType bitBucketEventType = getBitBucketEventType(httpHeaders);
        return bitBucketEventType == null ? httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT)
                                          : bitBucketEventType.getValue();
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

  public void validatePushEvent(WebhookSource webhookSource, HttpHeaders httpHeaders) {
    try {
      switch (webhookSource) {
        case GITHUB: {
          GitHubEventType gitHubEventType = getGitHubEventType(httpHeaders);
          if (GitHubEventType.PUSH == gitHubEventType) {
            return;
          }

          throw new InvalidRequestException(format("Push event expected. Found %s event", gitHubEventType), USER);
        }

        case GITLAB: {
          GitLabEventType gitLabEventType = getGitLabEventType(httpHeaders);
          if (GitLabEventType.PUSH == gitLabEventType) {
            return;
          }

          throw new InvalidRequestException(format("Push event expected. Found %s event", gitLabEventType), USER);
        }

        case BITBUCKET: {
          BitBucketEventType bitBucketEventType = getBitBucketEventType(httpHeaders);

          if (BitBucketEventType.PUSH == bitBucketEventType || BitBucketEventType.REFS_CHANGED == bitBucketEventType) {
            return;
          }

          throw new InvalidRequestException(format("Push event expected. Found %s event", bitBucketEventType), USER);
        }

        default:
          unhandled(webhookSource);
          throw new InvalidRequestException(format("Unhandled webhook source %s", webhookSource));
      }
    } catch (Exception ex) {
      logger.warn("Failed to validate push event for {} with headers {}", webhookSource, httpHeaders);
      throw ex;
    }
  }

  public boolean isGitPingEvent(HttpHeaders httpHeaders) {
    WebhookSource webhookSource = obtainWebhookSource(httpHeaders);
    WebhookEventType webhookEventType = null;

    switch (webhookSource) {
      case BITBUCKET:
        BitBucketEventType bitBucketEventType = getBitBucketEventType(httpHeaders);
        if (bitBucketEventType != null) {
          webhookEventType = bitBucketEventType.getEventType();
        }
        break;

      case GITHUB:
        GitHubEventType gitHubEventType = getGitHubEventType(httpHeaders);
        if (gitHubEventType != null) {
          webhookEventType = gitHubEventType.getEventType();
        }
        break;

      default:
        noop();
    }

    return WebhookEventType.PING == webhookEventType;
  }
}
