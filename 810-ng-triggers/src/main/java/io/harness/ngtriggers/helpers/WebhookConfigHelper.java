package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.AWS_CODECOMMIT;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.BITBUCKET;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITHUB;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITLAB;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerConfigV1;
import io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.event.AwsCodeCommitEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.event.AwsCodeCommitTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.event.BitbucketEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v1.git.GitAction;
import io.harness.ngtriggers.beans.source.webhook.v1.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v1.git.GitEvent;
import io.harness.ngtriggers.beans.source.webhook.v1.git.PayloadAware;
import io.harness.ngtriggers.beans.source.webhook.v1.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v1.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v1.github.event.GithubEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.GitlabSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.event.GitlabEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.event.GitlabTriggerEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class WebhookConfigHelper {
  public Map<WebhookSourceRepo, List<WebhookEvent>> getSourceRepoToEvent() {
    Map<WebhookSourceRepo, List<WebhookEvent>> map = new HashMap<>();
    map.put(WebhookSourceRepo.GITHUB, new ArrayList<>(WebhookEvent.githubEvents));
    map.put(WebhookSourceRepo.GITLAB, new ArrayList<>(WebhookEvent.gitlabEvents));
    map.put(WebhookSourceRepo.BITBUCKET, new ArrayList<>(WebhookEvent.bitbucketEvents));
    map.put(WebhookSourceRepo.AWS_CODECOMMIT, new ArrayList<>(WebhookEvent.awsCodeCommitEvents));

    return map;
  }

  public List<GithubTriggerEvent> getGithubTriggerEvents() {
    return Arrays.asList(GithubTriggerEvent.PUSH, GithubTriggerEvent.PULL_REQUEST, GithubTriggerEvent.ISSUE_COMMENT);
  }

  public List<GitlabTriggerEvent> getGitlabTriggerEvents() {
    return Arrays.asList(GitlabTriggerEvent.PUSH, GitlabTriggerEvent.MERGE_REQUEST);
  }

  public List<BitbucketTriggerEvent> getBitbucketTriggerEvents() {
    return Arrays.asList(BitbucketTriggerEvent.PUSH, BitbucketTriggerEvent.PULL_REQUEST);
  }

  public List<WebhookAction> getActionsList(WebhookSourceRepo sourceRepo, WebhookEvent event) {
    if (sourceRepo == WebhookSourceRepo.GITHUB) {
      return new ArrayList<>(WebhookAction.getGithubActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.BITBUCKET) {
      return new ArrayList<>(WebhookAction.getBitbucketActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.GITLAB) {
      return new ArrayList<>(WebhookAction.getGitLabActionForEvent(event));
    } else if (sourceRepo == WebhookSourceRepo.AWS_CODECOMMIT) {
      return new ArrayList<>(WebhookAction.getAwsCodeCommitActionForEvent(event));
    } else {
      return emptyList();
    }
  }

  public GitAware retrieveGitAware(WebhookTriggerConfigV1 webhookTriggerConfig) {
    if (!isGitSpec(webhookTriggerConfig)) {
      return null;
    }

    GitAware gitAware = null;
    if (webhookTriggerConfig.getType() == GITHUB) {
      GithubSpec githubSpec = (GithubSpec) webhookTriggerConfig.getSpec();
      GithubEventSpec gitEventSpec = githubSpec.getSpec();
      if (GitAware.class.isAssignableFrom(gitEventSpec.getClass())) {
        gitAware = (GitAware) gitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == GITLAB) {
      GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfig.getSpec();
      GitlabEventSpec gitlabEventSpec = gitlabSpec.getSpec();
      if (GitAware.class.isAssignableFrom(gitlabEventSpec.getClass())) {
        gitAware = (GitAware) gitlabEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == BITBUCKET) {
      BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfig.getSpec();
      BitbucketEventSpec bitbucketEventSpec = bitbucketSpec.getSpec();
      if (GitAware.class.isAssignableFrom(bitbucketEventSpec.getClass())) {
        gitAware = (GitAware) bitbucketEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == AWS_CODECOMMIT) {
      AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfig.getSpec();
      AwsCodeCommitEventSpec awsCodeCommitEventSpec = awsCodeCommitSpec.getSpec();
      if (GitAware.class.isAssignableFrom(awsCodeCommitEventSpec.getClass())) {
        gitAware = (GitAware) awsCodeCommitEventSpec;
      }
    }

    return gitAware;
  }

  public PayloadAware retrievePayloadAware(WebhookTriggerConfigV1 webhookTriggerConfig) {
    if (!isGitSpec(webhookTriggerConfig)) {
      return null;
    }

    PayloadAware payloadAware = null;
    if (webhookTriggerConfig.getType() == GITHUB) {
      GithubSpec githubSpec = (GithubSpec) webhookTriggerConfig.getSpec();
      GithubEventSpec gitEventSpec = githubSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(gitEventSpec.getClass())) {
        payloadAware = (PayloadAware) gitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == GITLAB) {
      GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfig.getSpec();
      GitlabEventSpec gitlabEventSpec = gitlabSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(gitlabEventSpec.getClass())) {
        payloadAware = (PayloadAware) gitlabEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == BITBUCKET) {
      BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfig.getSpec();
      BitbucketEventSpec bitbucketEventSpec = bitbucketSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(bitbucketEventSpec.getClass())) {
        payloadAware = (PayloadAware) bitbucketEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == AWS_CODECOMMIT) {
      AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfig.getSpec();
      AwsCodeCommitEventSpec awsCodeCommitEventSpec = awsCodeCommitSpec.getSpec();
      if (PayloadAware.class.isAssignableFrom(awsCodeCommitEventSpec.getClass())) {
        payloadAware = (PayloadAware) awsCodeCommitEventSpec;
      }
    } else if (webhookTriggerConfig.getType() == WebhookTriggerType.CUSTOM) {
      payloadAware = (PayloadAware) webhookTriggerConfig.getSpec();
    }

    return payloadAware;
  }

  public List<WebhookCondition> retrievePayloadConditions(WebhookTriggerConfigV1 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.getPayloadConditions();
    }

    return emptyList();
  }

  public List<WebhookCondition> retrieveHeaderConditions(WebhookTriggerConfigV1 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.getHeaderConditions();
    }

    return emptyList();
  }

  public String retrieveJexlExpression(WebhookTriggerConfigV1 webhookTriggerConfig) {
    PayloadAware payloadAware = null;
    payloadAware = retrievePayloadAware(webhookTriggerConfig);
    if (payloadAware != null) {
      return payloadAware.getJexlCondition();
    }

    return null;
  }

  public boolean isGitSpec(WebhookTriggerConfigV1 webhookTriggerConfig) {
    return webhookTriggerConfig.getType() == GITHUB || webhookTriggerConfig.getType() == GITLAB
        || webhookTriggerConfig.getType() == BITBUCKET || webhookTriggerConfig.getType() == AWS_CODECOMMIT;
  }

  public static List<GithubPRAction> getGithubPRAction() {
    return Arrays.asList(GithubPRAction.values());
  }

  public static List<GithubIssueCommentAction> getGithubIssueCommentAction() {
    return Arrays.asList(GithubIssueCommentAction.values());
  }

  public static List<GitlabPRAction> getGitlabPRAction() {
    return Arrays.asList(GitlabPRAction.values());
  }

  public static List<BitbucketPRAction> getBitbucketPRAction() {
    return Arrays.asList(BitbucketPRAction.values());
  }

  public static List<WebhookTriggerType> getWebhookTriggerType() {
    return Arrays.asList(WebhookTriggerType.values());
  }

  public static Map<WebhookTriggerType, Map<GitEvent, List<GitAction>>> getGitTriggerEventDetails() {
    Map<WebhookTriggerType, Map<GitEvent, List<GitAction>>> resposeMap = new HashMap<>();

    Map githubMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(GITHUB, githubMap);
    githubMap.put(GithubTriggerEvent.PUSH, emptyList());
    githubMap.put(GithubTriggerEvent.PULL_REQUEST, getGithubPRAction());
    githubMap.put(GithubTriggerEvent.ISSUE_COMMENT, getGithubIssueCommentAction());

    Map gitlabMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(GITLAB, gitlabMap);
    gitlabMap.put(GitlabTriggerEvent.PUSH, emptyList());
    gitlabMap.put(GitlabTriggerEvent.MERGE_REQUEST, getGitlabPRAction());

    Map bitbucketMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(BITBUCKET, bitbucketMap);
    bitbucketMap.put(BitbucketTriggerEvent.PUSH, emptyList());
    bitbucketMap.put(BitbucketTriggerEvent.PULL_REQUEST, getBitbucketPRAction());

    Map awsCodeCommitMap = new HashMap<GitEvent, List<GitAction>>();
    resposeMap.put(AWS_CODECOMMIT, awsCodeCommitMap);
    awsCodeCommitMap.put(AwsCodeCommitTriggerEvent.PUSH, emptyList());

    return resposeMap;
  }
}
