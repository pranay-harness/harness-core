package software.wings.yaml.trigger;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.PrAction;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class WebhookTriggerConditionHandler extends TriggerConditionYamlHandler<WebhookEventTriggerConditionYaml> {
  @Override
  public WebhookEventTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) bean;

    return WebhookEventTriggerConditionYaml.builder()
        .action(getYAMLActions(webHookTriggerCondition))
        .repositoryType(getBeanWebhookSourceForYAML(webHookTriggerCondition.getWebhookSource()))
        .eventType(getYAMLEventTypes(webHookTriggerCondition.getEventTypes()))
        .branchName(webHookTriggerCondition.getBranchName())
        .build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<WebhookEventTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    WebhookEventTriggerConditionYaml webhookConditionYaml = (WebhookEventTriggerConditionYaml) yaml;

    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder()
            .branchName(webhookConditionYaml.getBranchName())
            .eventTypes(getBeansEventTypes(webhookConditionYaml.getEventType()))
            .webhookSource(getBeanWebhookSource(webhookConditionYaml.getRepositoryType()))
            .build();

    if (webhookConditionYaml.getRepositoryType().equals("GitLab")) {
      webHookTriggerCondition.setActions(
          getPRActionTypes(webhookConditionYaml.getAction(), webhookConditionYaml.getRepositoryType()));
    } else if (webhookConditionYaml.getRepositoryType().equals("Bitbucket")) {
      webHookTriggerCondition.setBitBucketEvents(
          getBitBucketEventType(webhookConditionYaml.getAction(), webhookConditionYaml.getRepositoryType()));
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
      Validator.notNullCheck("webhook source is invalid or not supported", webhookSource);
    }

    return null;
  }

  private String getBeanWebhookSourceForYAML(WebhookSource webhookSource) {
    if (webhookSource == null) {
      return null;
    }
    if (webhookSource.equals(WebhookSource.GITHUB) || webhookSource.equals(WebhookSource.GITLAB)
        || webhookSource.equals(WebhookSource.BITBUCKET)) {
      return webhookSource.name();
    } else {
      Validator.notNullCheck("webhook source is invalid or not supported", webhookSource);
    }

    return null;
  }

  private List<WebhookEventType> getBeansEventTypes(List<String> eventTypes) {
    if (EmptyPredicate.isEmpty(eventTypes)) {
      return null;
    }
    return eventTypes.stream().map(eventType -> WebhookEventType.find(eventType)).collect(Collectors.toList());
  }

  private List<PrAction> getPRActionTypes(List<String> actions, String webhookSource) {
    if (webhookSource.equals("GitLab") && EmptyPredicate.isNotEmpty(actions)) {
      return actions.stream().map(action -> PrAction.find(action)).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<BitBucketEventType> getBitBucketEventType(List<String> actions, String webhookSource) {
    if (webhookSource.equals("Bitbucket") && EmptyPredicate.isNotEmpty(actions)) {
      return actions.stream().map(action -> BitBucketEventType.find(action)).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<String> getYAMLEventTypes(List<WebhookEventType> eventTypes) {
    if (EmptyPredicate.isNotEmpty(eventTypes)) {
      return eventTypes.stream().map(eventType -> { return eventType.getValue(); }).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  private List<String> getYAMLActions(WebHookTriggerCondition webHookTriggerCondition) {
    if (webHookTriggerCondition != null && webHookTriggerCondition.getWebhookSource() != null
        && webHookTriggerCondition.getWebhookSource().equals(WebhookSource.GITHUB)) {
      if (EmptyPredicate.isNotEmpty(webHookTriggerCondition.getActions())) {
        return webHookTriggerCondition.getActions()
            .stream()
            .map(prAction -> { return prAction.getValue(); })
            .collect(Collectors.toList());
      } else {
        return null;
      }
    } else if (webHookTriggerCondition != null && webHookTriggerCondition.getWebhookSource() != null
        && webHookTriggerCondition.getWebhookSource().equals(WebhookSource.BITBUCKET)) {
      if (EmptyPredicate.isNotEmpty(webHookTriggerCondition.getBitBucketEvents())) {
        return webHookTriggerCondition.getBitBucketEvents()
            .stream()
            .map(bitBucketEvent -> { return bitBucketEvent.getValue(); })
            .collect(Collectors.toList());
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public Class getYamlClass() {
    return WebhookEventTriggerConditionYaml.class;
  }
}
