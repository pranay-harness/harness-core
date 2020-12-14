package io.harness.ngtriggers.service.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.beans.target.TargetType.PIPELINE;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSetIntoPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.sanitizeRuntimeInput;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.ng.core.spring.NGTriggerRepository;
import io.harness.repositories.ng.core.spring.TriggerWebhookEventRepository;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
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
  private final NGPipelineService ngPipelineService;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  private static final String DUP_KEY_EXP_FORMAT_STRING = "Trigger [%s] already exists";

  @Override
  public NGTriggerEntity create(NGTriggerEntity ngTriggerEntity) {
    try {
      return ngTriggerRepository.save(ngTriggerEntity);
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format(DUP_KEY_EXP_FORMAT_STRING, ngTriggerEntity.getIdentifier()), WingsException.USER_SRE, e);
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
  public Page<NGTriggerEntity> listWebhookTriggers(
      String accountIdentifier, String repoUrl, boolean isDeleted, boolean enabledOnly) {
    return list(
        TriggerFilterHelper.createCriteriaForWebhookTriggerGetList(accountIdentifier, repoUrl, "", false, enabledOnly),
        Pageable.unpaged());
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

  @Override
  public void sanitizeRuntimeInputForTrigger(TriggerDetails triggerDetails) throws Exception {
    NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
    NgPipelineEntity ngPipelineEntity =
        ngPipelineService
            .get(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
                ngTriggerEntity.getProjectIdentifier(), ngTriggerEntity.getTargetIdentifier(), false)
            .orElse(null);

    TargetSpec targetSpec = triggerDetails.getNgTriggerConfig().getTarget().getSpec();
    if (PipelineTargetSpec.class.isAssignableFrom(targetSpec.getClass())) {
      PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
      String runtimeInput =
          sanitizeRuntimeInput(ngPipelineEntity.getYamlPipeline(), pipelineTargetSpec.getRuntimeInputYaml());
      pipelineTargetSpec.setRuntimeInputYaml(runtimeInput);
    }
    String yaml = YamlUtils.write(triggerDetails.getNgTriggerConfig()).replace("---\n", "");
    ngTriggerEntity.setYaml(yaml);
  }

  @Override
  public String generateFinalPipelineYmlForTrigger(TriggerDetails triggerDetails) {
    try {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      if (triggerDetails.getNgTriggerEntity().getTargetType() != PIPELINE) {
        throw new TriggerException(
            new StringBuilder(128)
                .append("Trigger has invalid Target Type. Only Pipeline Execution is supported.\n TriggerIdentifier: ")
                .append(ngTriggerEntity.getIdentifier())
                .append(", with org: ")
                .append(ngTriggerEntity.getOrgIdentifier())
                .append(", with ProjectId: ")
                .append(ngTriggerEntity.getProjectIdentifier())
                .toString(),
            WingsException.USER);
      }

      NgPipelineEntity ngPipelineEntity =
          ngPipelineService
              .get(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
                  ngTriggerEntity.getProjectIdentifier(), ngTriggerEntity.getTargetIdentifier(), false)
              .orElse(null);

      if (ngPipelineEntity == null) {
        throw new TriggerException(new StringBuilder(128)
                                       .append("Pipeline with identifier: ")
                                       .append(ngTriggerEntity.getTargetIdentifier())
                                       .append(", with org: ")
                                       .append(ngTriggerEntity.getOrgIdentifier())
                                       .append(", with ProjectId: ")
                                       .append(ngPipelineEntity.getProjectIdentifier())
                                       .append(", For Trigger: ")
                                       .append(ngTriggerEntity.getIdentifier())
                                       .append(" does not exists. Unable to continue trigger execution.")
                                       .toString(),
            USER);
      }
      sanitizeRuntimeInputForTrigger(triggerDetails);
      TargetSpec targetSpec = triggerDetails.getNgTriggerConfig().getTarget().getSpec();
      PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
      return mergeInputSetIntoPipeline(ngPipelineEntity.getYamlPipeline(), pipelineTargetSpec.getRuntimeInputYaml());
    } catch (Exception e) {
      log.error("Failure while trying to generate final Pipeline yml for Execution: ", e);
      throw new TriggerException(
          "Failure while trying to generate final Pipeline yml for Execution: " + e.getMessage(), USER);
    }
  }
}
