package software.wings.service.impl.yaml.handler.trigger;

import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.trigger.WebhookGitParam;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.GithubPayloadSourceYaml;
import software.wings.yaml.trigger.WebhookEventYaml;

import java.util.ArrayList;
import java.util.List;

public class GithubPayloadSourceYamlHandler extends PayloadSourceYamlHandler<GithubPayloadSourceYaml> {
  @Override
  public GithubPayloadSourceYaml toYaml(PayloadSource bean, String appId) {
    GitHubPayloadSource gitHubPayloadSource = (GitHubPayloadSource) bean;

    List<WebhookEventYaml> eventsYaml = new ArrayList<>();
    for (GitHubEventType gitHubEventType : gitHubPayloadSource.getGitHubEventTypes()) {
      eventsYaml.add(WebhookEventYaml.builder()
                         .eventType(gitHubEventType.getEventType().getValue())
                         .action(gitHubEventType.getValue())
                         .build());
    }

    WebhookGitParam webhookGitParam = gitHubPayloadSource.getWebhookGitParam();
    return GithubPayloadSourceYaml.builder()
        .customPayloadExpressions(gitHubPayloadSource.getCustomPayloadExpressions())
        .events(eventsYaml)
        .branchName(webhookGitParam.getBranchName())
        .filePaths(webhookGitParam.getFilePaths())
        .gitConnectorName(webhookGitParam.getGitConnectorId())
        .build();
  }

  @Override
  public PayloadSource upsertFromYaml(
      ChangeContext<GithubPayloadSourceYaml> changeContext, List<ChangeContext> changeSetContext) {
    GithubPayloadSourceYaml yaml = changeContext.getYaml();

    List<GitHubEventType> gitHubEvents = new ArrayList<>();
    for (WebhookEventYaml webhookEventYaml : yaml.getEvents()) {
      gitHubEvents.add(GitHubEventType.find(webhookEventYaml.getAction()));
    }

    WebhookGitParam webhookGitParam = WebhookGitParam.builder()
                                          .gitConnectorId(yaml.getGitConnectorName())
                                          .branchName(yaml.getBranchName())
                                          .filePaths(yaml.getFilePaths())
                                          .build();
    return GitHubPayloadSource.builder()
        .customPayloadExpressions(yaml.getCustomPayloadExpressions())
        .gitHubEventTypes(gitHubEvents)
        .webhookGitParam(webhookGitParam)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return GithubPayloadSourceYaml.class;
  }
}
