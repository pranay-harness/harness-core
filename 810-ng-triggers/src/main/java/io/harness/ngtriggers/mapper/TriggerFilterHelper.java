package io.harness.ngtriggers.mapper;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory.TriggerEventHistoryKeys;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class TriggerFilterHelper {
  public Criteria createCriteriaForGetList(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, NGTriggerType type, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountIdentifier)) {
      criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(targetIdentifier)) {
      criteria.and(NGTriggerEntityKeys.targetIdentifier).is(targetIdentifier);
    }
    criteria.and(NGTriggerEntityKeys.deleted).is(deleted);

    if (type != null) {
      criteria.and(NGTriggerEntityKeys.type).is(type);
    }
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Criteria createCriteriaForCustomWebhookTriggerGetList(TriggerWebhookEvent triggerWebhookEvent,
      String decryptedAuthToken, String searchTerm, boolean deleted, boolean enabled) {
    Criteria criteria = createCriteriaForWebhookTriggerGetList(triggerWebhookEvent.getAccountId(),
        triggerWebhookEvent.getOrgIdentifier(), triggerWebhookEvent.getProjectIdentifier(), emptyList(), searchTerm,
        deleted, enabled);
    if (triggerWebhookEvent.getSourceRepoType().equalsIgnoreCase(WebhookSourceRepo.CUSTOM.name())) {
      criteria.and("metadata.webhook.type").is("CUSTOM");
      criteria.and("metadata.webhook.custom.customAuthTokenType")
          .is("inline")
          .and("metadata.webhook.custom.customAuthTokenValue")
          .is(decryptedAuthToken);
    }

    return criteria;
  }

  public Criteria createCriteriaForWebhookTriggerGetList(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<String> repoURLs, String searchTerm, boolean deleted, boolean enabledOnly) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountIdentifier)) {
      criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(repoURLs)) {
      criteria.and("metadata.webhook.repoURL").in(repoURLs);
    }
    criteria.and(NGTriggerEntityKeys.deleted).is(deleted);
    criteria.and(NGTriggerEntityKeys.type).is(NGTriggerType.WEBHOOK);
    if (enabledOnly) {
      criteria.and(NGTriggerEntityKeys.enabled).is(true);
    }

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Update getUpdateOperations(NGTriggerEntity triggerEntity) {
    Update update = new Update();
    update.set(NGTriggerEntityKeys.name, triggerEntity.getName());
    update.set(NGTriggerEntityKeys.identifier, triggerEntity.getIdentifier());
    update.set(NGTriggerEntityKeys.description, triggerEntity.getDescription());
    update.set(NGTriggerEntityKeys.yaml, triggerEntity.getYaml());

    update.set(NGTriggerEntityKeys.type, triggerEntity.getType());
    update.set(NGTriggerEntityKeys.metadata, triggerEntity.getMetadata());
    update.set(NGTriggerEntityKeys.enabled, triggerEntity.getEnabled());
    update.set(NGTriggerEntityKeys.tags, triggerEntity.getTags());
    update.set(NGTriggerEntityKeys.deleted, false);

    return update;
  }

  public Update getUpdateOperations(TriggerWebhookEvent triggerWebhookEvent) {
    Update update = new Update();
    update.set(TriggerWebhookEventsKeys.attemptCount, triggerWebhookEvent.getAttemptCount());
    update.set(TriggerWebhookEventsKeys.processing, triggerWebhookEvent.isProcessing());
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(NGTriggerEntityKeys.deleted, true);
    return update;
  }

  public Criteria createCriteriaForTriggerEventCountLastNDays(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String triggerIdentifier, String targetIdentifier, long startTime) {
    Criteria criteria = new Criteria();
    criteria.and(TriggerEventHistoryKeys.accountId).is(accountIdentifier);
    criteria.and(TriggerEventHistoryKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(TriggerEventHistoryKeys.projectIdentifier).is(projectIdentifier);
    criteria.and(TriggerEventHistoryKeys.triggerIdentifier).is(triggerIdentifier);
    criteria.and(TriggerEventHistoryKeys.targetIdentifier).is(targetIdentifier);
    criteria.and(TriggerEventHistoryKeys.createdAt).gte(startTime);

    return criteria;
  }
}
