package io.harness.ngtriggers.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TriggerException;
import io.harness.network.SafeHttpCall;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.repositories.ng.core.spring.NGTriggerRepository;
import io.harness.repositories.ng.core.spring.TriggerWebhookEventRepository;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGTriggerServiceImpl implements NGTriggerService {
  private final NGTriggerRepository ngTriggerRepository;
  private final TriggerWebhookEventRepository webhookEventQueueRepository;
  private final ConnectorResourceClient connectorResourceClient;

  private static final String DUP_KEY_EXP_FORMAT_STRING = "Trigger [%s] already exists";

  @Override
  public NGTriggerEntity create(NGTriggerEntity ngTriggerEntity) {
    try {
      return ngTriggerRepository.save(ngTriggerEntity);
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, ngTriggerEntity.getIdentifier()), USER_SRE, e);
    }
  }

  @Override
  public Optional<NGTriggerEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, String identifier, boolean deleted) {
    return ngTriggerRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndTargetIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, targetIdentifier, identifier, !deleted);
  }

  @Override
  public NGTriggerEntity update(NGTriggerEntity ngTriggerEntity) {
    Criteria criteria = getTriggerEqualityCriteria(ngTriggerEntity, false);
    NGTriggerEntity updatedEntity = ngTriggerRepository.update(criteria, ngTriggerEntity);
    if (updatedEntity == null) {
      throw new InvalidRequestException(
          String.format("NGTrigger [%s] couldn't be updated or doesn't exist", ngTriggerEntity.getIdentifier()));
    }
    return updatedEntity;
  }

  @Override
  public boolean updateTriggerStatus(NGTriggerEntity ngTriggerEntity, boolean status) {
    Criteria criteria = getTriggerEqualityCriteria(ngTriggerEntity, false);
    ngTriggerEntity.setEnabled(status);

    NGTriggerEntity updatedEntity = ngTriggerRepository.update(criteria, ngTriggerEntity);
    if (updatedEntity != null) {
      return updatedEntity.getEnabled();
    } else {
      throw new InvalidRequestException(
          String.format("NGTrigger [%s] couldn't be updated or doesn't exist", ngTriggerEntity.getIdentifier()));
    }
  }

  @Override
  public Page<NGTriggerEntity> list(Criteria criteria, Pageable pageable) {
    return ngTriggerRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String targetIdentifier,
      String identifier, Long version) {
    Criteria criteria = getTriggerEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, targetIdentifier, identifier, false, version);
    UpdateResult deleteResult = ngTriggerRepository.delete(criteria);
    if (!deleteResult.wasAcknowledged() || deleteResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(String.format("NGTrigger [%s] couldn't be deleted", identifier));
    }
    return true;
  }

  @Override
  public TriggerWebhookEvent addEventToQueue(TriggerWebhookEvent webhookEventQueueRecord) {
    try {
      return webhookEventQueueRepository.save(webhookEventQueueRecord);
    } catch (Exception e) {
      throw new InvalidRequestException("Webhook event could not be received");
    }
  }

  @Override
  public TriggerWebhookEvent updateTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord) {
    Criteria criteria = getTriggerWebhookEventEqualityCriteria(webhookEventQueueRecord);
    TriggerWebhookEvent updatedEntity = webhookEventQueueRepository.update(criteria, webhookEventQueueRecord);
    if (updatedEntity == null) {
      throw new InvalidRequestException(
          "TriggerWebhookEvent with uuid " + webhookEventQueueRecord.getUuid() + " could not be updated");
    }
    return updatedEntity;
  }

  @Override
  public void deleteTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord) {
    webhookEventQueueRepository.delete(webhookEventQueueRecord);
  }

  @Override
  public List<NGTriggerEntity> findTriggersForCustomWehbook(
      TriggerWebhookEvent triggerWebhookEvent, String decryptedAuthToken, boolean isDeleted, boolean enabled) {
    Page<NGTriggerEntity> triggersPage = list(TriggerFilterHelper.createCriteriaForCustomWebhookTriggerGetList(
                                                  triggerWebhookEvent, decryptedAuthToken, EMPTY, isDeleted, enabled),
        Pageable.unpaged());

    return triggersPage.get().collect(Collectors.toList());
  }

  @Override
  public List<NGTriggerEntity> listEnabledTriggersForCurrentProject(
      String accountId, String orgIdentifier, String projectIdentifier) {
    Optional<List<NGTriggerEntity>> enabledTriggerForProject;

    // Now kept for backward compatibility, but will be changed soon to validate for non-empty project and
    // orgIdentifier.
    if (isNotEmpty(projectIdentifier) && isNotEmpty(orgIdentifier)) {
      enabledTriggerForProject = ngTriggerRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnabled(
          accountId, orgIdentifier, projectIdentifier, true);
    } else if (isNotEmpty(orgIdentifier)) {
      enabledTriggerForProject =
          ngTriggerRepository.findByAccountIdAndOrgIdentifierAndEnabled(accountId, orgIdentifier, true);
    } else {
      enabledTriggerForProject = ngTriggerRepository.findByAccountIdAndEnabled(accountId, true);
    }

    if (enabledTriggerForProject.isPresent()) {
      return enabledTriggerForProject.get();
    }

    return emptyList();
  }

  @Override
  public List<ConnectorResponseDTO> fetchConnectorsByFQN(String accountIdentifier, List<String> fqns) {
    if (isEmpty(fqns)) {
      return emptyList();
    }
    try {
      return SafeHttpCall.executeWithExceptions(connectorResourceClient.listConnectorByFQN(accountIdentifier, fqns))
          .getData();
    } catch (Exception e) {
      log.error("Failed while retrieving connectors", e);
      throw new TriggerException("Failed while retrieving connectors" + e.getMessage(), e, USER_SRE);
    }
  }

  @Override
  public void validateTriggerConfig(TriggerDetails triggerDetails) {
    // TODO: come up with a comprehensive list of back-end checks for the trigger details for which an error
    // will be returned if certain conditions are not met. Either use this as a gateway or spin off a specific class
    // for the validation.

    // trigger source validation
    NGTriggerSource triggerSource = triggerDetails.getNgTriggerConfig().getSource();
    switch (triggerSource.getType()) {
      case WEBHOOK:
        return; // TODO(adwait): define trigger source validation
      case SCHEDULED:
        ScheduledTriggerConfig scheduledTriggerConfig = (ScheduledTriggerConfig) triggerSource.getSpec();
        CronTriggerSpec cronTriggerSpec = (CronTriggerSpec) scheduledTriggerConfig.getSpec();
        CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        Cron cron = cronParser.parse(cronTriggerSpec.getExpression());
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextTime = executionTime.nextExecution(ZonedDateTime.now());
        if (!nextTime.isPresent()) {
          throw new InvalidArgumentsException("cannot find iteration time!");
        }
        return;
      case NEW_ARTIFACT: // fall through
      default:
        return; // not implemented
    }
  }

  private Criteria getTriggerWebhookEventEqualityCriteria(TriggerWebhookEvent webhookEventQueueRecord) {
    return Criteria.where(TriggerWebhookEventsKeys.uuid).is(webhookEventQueueRecord.getUuid());
  }

  private Criteria getTriggerEqualityCriteria(NGTriggerEntity ngTriggerEntity, boolean deleted) {
    return getTriggerEqualityCriteria(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
        ngTriggerEntity.getProjectIdentifier(), ngTriggerEntity.getTargetIdentifier(), ngTriggerEntity.getIdentifier(),
        deleted, ngTriggerEntity.getVersion());
  }

  private Criteria getTriggerEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, String identifier, boolean deleted, Long version) {
    Criteria criteria = Criteria.where(NGTriggerEntityKeys.accountId)
                            .is(accountId)
                            .and(NGTriggerEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(NGTriggerEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(NGTriggerEntityKeys.targetIdentifier)
                            .is(targetIdentifier)
                            .and(NGTriggerEntityKeys.identifier)
                            .is(identifier)
                            .and(NGTriggerEntityKeys.deleted)
                            .is(deleted);
    if (version != null) {
      criteria.and(NGTriggerEntityKeys.version).is(version);
    }
    return criteria;
  }
}
