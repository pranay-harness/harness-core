package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.GithubAction;
import software.wings.beans.trigger.ReleaseAction;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class WebhookTriggerConditionHandler extends TriggerConditionYamlHandler<WebhookEventTriggerConditionYaml> {
  private static final String PACKAGE_STRING = "package:";
  private static final String SPLITTER = ":";
  @Override
  public WebhookEventTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) bean;

    return WebhookEventTriggerConditionYaml.builder()
        .action(getYAMLActions(webHookTriggerCondition))
        .releaseActions(getYAMLReleaseActions(webHookTriggerCondition))
        .repositoryType(getBeanWebhookSourceForYAML(webHookTriggerCondition.getWebhookSource()))
        .eventType(getYAMLEventTypes(webHookTriggerCondition.getEventTypes()))
        .branchName(webHookTriggerCondition.getBranchRegex())
        .build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<WebhookEventTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();

    WebhookEventTriggerConditionYaml webhookConditionYaml = (WebhookEventTriggerConditionYaml) yaml;

    return fromYAML(webhookConditionYaml);
  }

  public WebHookTriggerCondition fromYAML(WebhookEventTriggerConditionYaml webhookConditionYaml) {
    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder()
            .branchRegex(webhookConditionYaml.getBranchName())
            .eventTypes(getBeansEventTypes(webhookConditionYaml.getEventType()))
            .webhookSource(getBeanWebhookSource(webhookConditionYaml.getRepositoryType()))
            .build();

    if (EmptyPredicate.isNotEmpty(webhookConditionYaml.getRepositoryType())) {
      if (webhookConditionYaml.getRepositoryType().equals(GITHUB.name())) {
        webHookTriggerCondition.setActions(getPRActionTypes(webhookConditionYaml.getAction(),
            webhookConditionYaml.getRepositoryType(), webhookConditionYaml.getEventType()));
        webHookTriggerCondition.setReleaseActions(
            getReleaseActionTypes(webhookConditionYaml.getReleaseActions(), webhookConditionYaml.getRepositoryType()));
      } else if (webhookConditionYaml.getRepositoryType().equals(BITBUCKET.name())) {
        webHookTriggerCondition.setBitBucketEvents(
            getBitBucketEventType(webhookConditionYaml.getAction(), webhookConditionYaml.getRepositoryType()));
      }
    }

    return webHookTriggerCondition;
  }

  private WebhookSource getBeanWebhookSource(String webhookSource) {
    if (EmptyPredicate.isEmpty(webhookSource)) {
      return null;
    }
    if (webhookSource.equals("GITLAB")) {
      return WebhookSource.GITLAB;
    } else if (webhookSource.equals("GITHUB")) {
      return WebhookSource.GITHUB;
    } else if (webhookSource.equals("BITBUCKET")) {
      return WebhookSource.BITBUCKET;
    } else {
      notNullCheck("webhook source is invalid or not supported", webhookSource);
    }

    return null;
  }

  private String getBeanWebhookSourceForYAML(WebhookSource webhookSource) {
    if (webhookSource == null) {
      return null;
    }
    if (webhookSource == WebhookSource.GITHUB || webhookSource == WebhookSource.GITLAB
        || webhookSource == WebhookSource.BITBUCKET) {
      return webhookSource.name();
    } else {
      notNullCheck("webhook source is invalid or not supported", webhookSource);
    }

    return null;
  }

  private List<WebhookEventType> getBeansEventTypes(List<String> eventTypes) {
    if (EmptyPredicate.isEmpty(eventTypes)) {
      return null;
    }
    return eventTypes.stream().map(WebhookEventType::find).collect(Collectors.toList());
  }

  private List<GithubAction> getPRActionTypes(List<String> actions, String webhookSource, List<String> eventType) {
    if (EmptyPredicate.isNotEmpty(actions) && EmptyPredicate.isNotEmpty(webhookSource)
        && webhookSource.equals(GITHUB.name())) {
      if (eventType != null && eventType.contains("package")) {
        return actions.stream().map(action -> GithubAction.find(PACKAGE_STRING + action)).collect(Collectors.toList());
      }
      return actions.stream().map(GithubAction::find).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<ReleaseAction> getReleaseActionTypes(List<String> actions, String webhookSource) {
    if (EmptyPredicate.isNotEmpty(actions) && EmptyPredicate.isNotEmpty(webhookSource)
        && webhookSource.equals(GITHUB.name())) {
      return actions.stream().map(ReleaseAction::find).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<BitBucketEventType> getBitBucketEventType(List<String> actions, String webhookSource) {
    if (EmptyPredicate.isNotEmpty(actions) && EmptyPredicate.isNotEmpty(webhookSource)
        && webhookSource.equals("BITBUCKET")) {
      return actions.stream().map(BitBucketEventType::find).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<String> getYAMLEventTypes(List<WebhookEventType> eventTypes) {
    if (EmptyPredicate.isNotEmpty(eventTypes)) {
      return eventTypes.stream().map(WebhookEventType::getValue).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<String> getYAMLActions(WebHookTriggerCondition webHookTriggerCondition) {
    if (webHookTriggerCondition != null && webHookTriggerCondition.getWebhookSource() != null
        && webHookTriggerCondition.getWebhookSource() == GITHUB) {
      if (EmptyPredicate.isNotEmpty(webHookTriggerCondition.getActions())) {
        if (webHookTriggerCondition.getEventTypes() != null
            && webHookTriggerCondition.getEventTypes().contains(WebhookEventType.PACKAGE)) {
          List<String> yamlActions = new ArrayList<>();
          for (GithubAction githubAction : webHookTriggerCondition.getActions()) {
            if (githubAction.getValue() != null && githubAction.getValue().split(SPLITTER).length > 1) {
              yamlActions.add(githubAction.getValue().split(SPLITTER)[1]);
            }
          }
          return yamlActions;
        }
        return webHookTriggerCondition.getActions().stream().map(GithubAction::getValue).collect(Collectors.toList());
      } else {
        return null;
      }
    } else if (webHookTriggerCondition != null && webHookTriggerCondition.getWebhookSource() != null
        && webHookTriggerCondition.getWebhookSource() == WebhookSource.BITBUCKET) {
      if (EmptyPredicate.isNotEmpty(webHookTriggerCondition.getBitBucketEvents())) {
        return webHookTriggerCondition.getBitBucketEvents()
            .stream()
            .map(BitBucketEventType::getValue)
            .collect(Collectors.toList());
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private List<String> getYAMLReleaseActions(WebHookTriggerCondition webHookTriggerCondition) {
    if (webHookTriggerCondition != null && webHookTriggerCondition.getWebhookSource() != null
        && webHookTriggerCondition.getWebhookSource() == GITHUB) {
      if (EmptyPredicate.isNotEmpty(webHookTriggerCondition.getReleaseActions())) {
        return webHookTriggerCondition.getReleaseActions()
            .stream()
            .map(ReleaseAction::getValue)
            .collect(Collectors.toList());
      }
    }
    return null;
  }

  @Override
  public Class getYamlClass() {
    return WebhookEventTriggerConditionYaml.class;
  }
}
