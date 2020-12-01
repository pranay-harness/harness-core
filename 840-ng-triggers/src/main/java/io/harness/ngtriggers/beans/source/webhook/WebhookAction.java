package io.harness.ngtriggers.beans.source.webhook;

import static java.util.Collections.emptySet;

import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum WebhookAction {
  @JsonProperty("created") CREATED("create", "created"),
  @JsonProperty("closed") CLOSED("close", "closed"),
  @JsonProperty("edited") EDITED("edit", "edited"),
  @JsonProperty("edited") UPDATED("update", "updated"),
  @JsonProperty("opened") OPENED("open", "opened"),
  @JsonProperty("reopened") REOPENED("reopen", "reopened"),
  @JsonProperty("labeled") LABELED("label", "labeled"),
  @JsonProperty("unlabeled") UNLABELED("unlabel", "unlabeled"),
  @JsonProperty("deleted") DELETED("delete", "deleted"),
  @JsonProperty("synchronized") SYNCHRONIZED("sync", "synchronized"),
  @JsonProperty("synced") SYNC("sync", "synced"),
  @JsonProperty("merged") MERGED("merge", "merged"),

  @JsonProperty("sync") GITLAB_SYNC("sync", "sync"),
  @JsonProperty("open") GITLAB_OPEN("open", "open"),
  @JsonProperty("close") GITLAB_CLOSE("close", "close"),
  @JsonProperty("reopen") GITLAB_REOPEN("reopen", "reopen"),
  @JsonProperty("merge") GITLAB_MERGED("merge", "merge"),
  @JsonProperty("update") GITLAB_UPDATED("update", "update"),

  @JsonProperty("pull request created") PULL_REQUEST_CREATED("open", "pull request created"),
  @JsonProperty("pull request updated") PULL_REQUEST_UPDATED("sync", "pull request updated"),
  @JsonProperty("pull request merged") PULL_REQUEST_MERGED("merge", "pull request merged"),
  @JsonProperty("pull request declined") PULL_REQUEST_DECLINED("close", "pull request declined");

  // TODO: Add more support for more actions we need to support
  private String value;
  private String parsedValue;

  WebhookAction(String parsedValue, String value) {
    this.parsedValue = parsedValue;
    this.value = value;
    EventActionHolder.map.put(value, this);
  }

  public String getParsedValue() {
    return parsedValue;
  }

  public String getValue() {
    return value;
  }
  private static class EventActionHolder { static Map<String, WebhookAction> map = new HashMap<>(); }

  public static WebhookAction find(String val) {
    WebhookAction action = EventActionHolder.map.get(val);
    if (action == null) {
      throw new InvalidRequestException(String.format("Unsupported Webhook action %s.", val));
    }
    return action;
  }

  public static Set<WebhookAction> getGithubActionForEvent(WebhookEvent event) {
    switch (event) {
      case PULL_REQUEST:
        return EnumSet.of(CLOSED, EDITED, LABELED, OPENED, REOPENED, SYNCHRONIZED, UNLABELED);
        //      case PACKAGE:
        //        return EnumSet.of(PUBLISHED);
        //      case RELEASE:
        //        return EnumSet.of(CREATED, DELETED, EDITED, PRE_RELEASED, PUBLISHED, RELEASED, UNPUBLISHED);
      case PUSH:
      case DELETE:
        return emptySet();
      default:
        throw new InvalidRequestException("Event " + event.name() + " not a github event");
    }
  }

  public static Set<WebhookAction> getBitbucketActionForEvent(WebhookEvent event) {
    switch (event) {
      case PULL_REQUEST:
        return EnumSet.of(PULL_REQUEST_CREATED, PULL_REQUEST_UPDATED, PULL_REQUEST_MERGED, PULL_REQUEST_DECLINED);
      case REPOSITORY:
      case ISSUE:
        return emptySet();
      default:
        throw new InvalidRequestException("Event " + event.name() + " not a bitbucket event");
    }
  }

  public static Set<WebhookAction> getGitLabActionForEvent(WebhookEvent event) {
    switch (event) {
      case MERGE_REQUEST:
        return EnumSet.of(GITLAB_OPEN, GITLAB_CLOSE, GITLAB_REOPEN, GITLAB_MERGED, GITLAB_UPDATED, GITLAB_SYNC);
      case PUSH:
      case DELETE:
        return emptySet();
      default:
        throw new InvalidRequestException("Event " + event.name() + " not a gitlab event");
    }
  }
}
