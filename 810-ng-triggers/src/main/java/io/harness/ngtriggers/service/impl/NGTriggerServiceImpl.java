package io.harness.ngtriggers.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TriggerException;
import io.harness.network.SafeHttpCall;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.WebhookEventProcessingDetails;
import io.harness.ngtriggers.beans.dto.WebhookEventProcessingDetails.WebhookEventProcessingDetailsBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.NGTriggerWebhookRegistrationService;
import io.harness.repositories.spring.NGTriggerRepository;
import io.harness.repositories.spring.TriggerEventHistoryRepository;
import io.harness.repositories.spring.TriggerWebhookEventRepository;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
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
@OwnedBy(PIPELINE)
public class NGTriggerServiceImpl implements NGTriggerService {
  public static final long TRIGGER_CURRENT_YML_VERSION = 2l;
  private final NGTriggerRepository ngTriggerRepository;
  private final TriggerWebhookEventRepository webhookEventQueueRepository;
  private final TriggerEventHistoryRepository triggerEventHistoryRepository;
  private final ConnectorResourceClient connectorResourceClient;
  private final NGTriggerWebhookRegistrationService ngTriggerWebhookRegistrationService;
  private final ExecutorService executorService;

  private static final String DUP_KEY_EXP_FORMAT_STRING = "Trigger [%s] already exists";

  @Override
  public NGTriggerEntity create(NGTriggerEntity ngTriggerEntity) {
    try {
      NGTriggerEntity savedNgTriggerEntity = ngTriggerRepository.save(ngTriggerEntity);
      registerWebhookAsync(ngTriggerEntity);
      return savedNgTriggerEntity;
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, ngTriggerEntity.getIdentifier()), USER_SRE, e);
    }
  }

  private void registerWebhookAsync(NGTriggerEntity ngTriggerEntity) {
    if (!ngTriggerEntity.getAutoRegister()) {
      return;
    }
    executorService.submit(() -> {
      WebhookRegistrationStatus registrationStatus =
          ngTriggerWebhookRegistrationService.registerWebhook(ngTriggerEntity);
      updateWebhookRegistrationStatus(ngTriggerEntity, registrationStatus);
    });
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
    ngTriggerEntity.setYmlVersion(TRIGGER_CURRENT_YML_VERSION);
    Criteria criteria = getTriggerEqualityCriteria(ngTriggerEntity, false);
    NGTriggerEntity updatedEntity = ngTriggerRepository.update(criteria, ngTriggerEntity);
    if (updatedEntity == null) {
      throw new InvalidRequestException(
          String.format("NGTrigger [%s] couldn't be updated or doesn't exist", ngTriggerEntity.getIdentifier()));
    }
    registerWebhookAsync(updatedEntity);
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
  public boolean deleteAllForPipeline(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    String pipelineRef = new StringBuilder(128)
                             .append(accountId)
                             .append('/')
                             .append(orgIdentifier)
                             .append('/')
                             .append(projectIdentifier)
                             .append('/')
                             .append(pipelineIdentifier)
                             .toString();

    final AtomicBoolean exceptionOccured = new AtomicBoolean(false);

    try {
      Optional<List<NGTriggerEntity>> nonDeletedTriggerForPipeline =
          ngTriggerRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndTargetIdentifierAndDeletedNot(
              accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, true);

      if (nonDeletedTriggerForPipeline.isPresent()) {
        log.info("Deleting triggers for pipeline-deletion-event. PipelineRef: " + pipelineRef);
        List<NGTriggerEntity> ngTriggerEntities = nonDeletedTriggerForPipeline.get();
        String triggerRef = new StringBuilder(128)
                                .append(accountId)
                                .append('/')
                                .append(orgIdentifier)
                                .append('/')
                                .append(projectIdentifier)
                                .append('/')
                                .append(pipelineIdentifier)
                                .append('/')
                                .append("{trigger}")
                                .toString();

        ngTriggerEntities.forEach(ngTriggerEntity -> {
          try {
            log.info("Deleting triggers for pipeline-deletion-event. TriggerRef: "
                + triggerRef.replace("{trigger}", ngTriggerEntity.getIdentifier()));
            delete(
                accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, ngTriggerEntity.getIdentifier(), null);
          } catch (Exception e) {
            log.error("Error while deleting trigger while processing pipeline-delete-event. TriggerRef: "
                + triggerRef.replace("{trigger}", ngTriggerEntity.getIdentifier()));
            exceptionOccured.set(true);
          }
        });
      } else {
        log.info("No non-deleted Trigger found while processing pipeline-deletion-event. PipelineRef: " + pipelineRef);
      }
    } catch (Exception e) {
      log.error("Error while deleting triggers while processing pipeline-delete-event. PipelineRef: " + pipelineRef);
      exceptionOccured.set(true);
    }

    return !exceptionOccured.get();
  }

  @Override
  public WebhookEventProcessingDetails fetchTriggerEventHistory(String accountId, String eventId) {
    TriggerEventHistory triggerEventHistory =
        triggerEventHistoryRepository.findByAccountIdAndEventCorrelationId(accountId, eventId);
    WebhookEventProcessingDetailsBuilder builder =
        WebhookEventProcessingDetails.builder().eventId(eventId).accountIdentifier(accountId);
    if (triggerEventHistory == null) {
      builder.eventFound(false);
    } else {
      builder.eventFound(true)
          .orgIdentifier(triggerEventHistory.getOrgIdentifier())
          .projectIdentifier(triggerEventHistory.getProjectIdentifier())
          .triggerIdentifier(triggerEventHistory.getTriggerIdentifier())
          .pipelineIdentifier(triggerEventHistory.getTargetIdentifier())
          .exceptionOccured(triggerEventHistory.isExceptionOccurred())
          .status(triggerEventHistory.getFinalStatus())
          .message(triggerEventHistory.getMessage())
          .payload(triggerEventHistory.getPayload())
          .eventCreatedAt(triggerEventHistory.getCreatedAt());

      if (triggerEventHistory.getTargetExecutionSummary() != null) {
        builder.pipelineExecutionId(triggerEventHistory.getTargetExecutionSummary().getPlanExecutionId())
            .runtimeInput(triggerEventHistory.getTargetExecutionSummary().getRuntimeInput());
      }
    }

    return builder.build();
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
      TriggerWebhookEvent triggerWebhookEvent, boolean isDeleted, boolean enabled) {
    Page<NGTriggerEntity> triggersPage = list(TriggerFilterHelper.createCriteriaForCustomWebhookTriggerGetList(
                                                  triggerWebhookEvent, EMPTY, isDeleted, enabled),
        Pageable.unpaged());

    return triggersPage.get().collect(Collectors.toList());
  }

  @Override
  public List<NGTriggerEntity> findTriggersForWehbookBySourceRepoType(
      TriggerWebhookEvent triggerWebhookEvent, boolean isDeleted, boolean enabled) {
    Page<NGTriggerEntity> triggersPage = list(TriggerFilterHelper.createCriteriaFormWebhookTriggerGetListByRepoType(
                                                  triggerWebhookEvent, EMPTY, isDeleted, enabled),
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
      enabledTriggerForProject =
          ngTriggerRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnabledAndDeletedNot(
              accountId, orgIdentifier, projectIdentifier, true, true);
    } else if (isNotEmpty(orgIdentifier)) {
      enabledTriggerForProject = ngTriggerRepository.findByAccountIdAndOrgIdentifierAndEnabledAndDeletedNot(
          accountId, orgIdentifier, true, true);
    } else {
      enabledTriggerForProject = ngTriggerRepository.findByAccountIdAndEnabledAndDeletedNot(accountId, true, true);
    }

    if (enabledTriggerForProject.isPresent()) {
      return enabledTriggerForProject.get();
    }

    return emptyList();
  }

  @Override
  public List<NGTriggerEntity> listEnabledTriggersForAccount(String accountId) {
    return listEnabledTriggersForCurrentProject(accountId, null, null);
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
    if (isBlank(triggerDetails.getNgTriggerEntity().getIdentifier())) {
      throw new InvalidArgumentsException("Identifier can not be empty");
    }

    if (isBlank(triggerDetails.getNgTriggerEntity().getName())) {
      throw new InvalidArgumentsException("Name can not be empty");
    }

    NGTriggerSourceV2 triggerSource = triggerDetails.getNgTriggerConfigV2().getSource();
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
      default:
        return; // not implemented
    }
  }

  private void updateWebhookRegistrationStatus(
      NGTriggerEntity ngTriggerEntity, WebhookRegistrationStatus registrationStatus) {
    Criteria criteria = getTriggerEqualityCriteria(ngTriggerEntity, false);
    ngTriggerEntity.getMetadata().getWebhook().setRegistrationStatus(registrationStatus);

    NGTriggerEntity updatedEntity = ngTriggerRepository.update(criteria, ngTriggerEntity);
    if (updatedEntity == null) {
      throw new InvalidRequestException(
          String.format("NGTrigger [%s] couldn't be updated or doesn't exist", ngTriggerEntity.getIdentifier()));
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
