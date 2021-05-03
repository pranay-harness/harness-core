package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.common.beans.NGTag.NGTagKeys;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.observer.Subject;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.mappers.PipelineYamlDtoMapper;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.variables.VariableCreatorMergeService;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.repositories.pipeline.PMSPipelineRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceImpl implements PMSPipelineService {
  @Inject private PMSPipelineRepository pmsPipelineRepository;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;
  @Inject private VariableCreatorMergeService variableCreatorMergeService;
  @Inject private PMSPipelineServiceHelper pmsPipelineServiceHelper;
  @Inject private PMSPipelineServiceStepHelper pmsPipelineServiceStepHelper;
  @Inject @Getter private final Subject<PipelineActionObserver> pipelineSubject = new Subject<>();

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
          pipelineEntity.getIdentifier());

      pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);

      return pmsPipelineRepository.save(pipelineEntity, PipelineYamlDtoMapper.toDto(pipelineEntity));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format(DUP_KEY_EXP_FORMAT_STRING, pipelineEntity.getIdentifier(),
                                            pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (IOException | ProducerShutdownException exception) {
      throw new InvalidRequestException(String.format(
          "Unknown exception occurred while updating pipeline with id: [%s]. Please contact Harness Support",
          pipelineEntity.getIdentifier()));
    }
  }

  @Override
  public Optional<PipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    return pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, identifier, !deleted);
  }

  @Override
  public PipelineEntity update(PipelineEntity pipelineEntity) {
    try {
      PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());

      pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);

      Criteria criteria =
          PMSPipelineServiceHelper.getPipelineEqualityCriteria(pipelineEntity, pipelineEntity.getDeleted());
      PipelineEntity updateResult = pmsPipelineRepository.update(criteria, pipelineEntity);
      if (updateResult == null) {
        throw new InvalidRequestException(format(
            "Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
            pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
      }

      return updateResult;
    } catch (IOException | ProducerShutdownException exception) {
      throw new InvalidRequestException(String.format(
          "Unknown exception occurred while updating pipeline with id: [%s]. Please contact Harness Support",
          pipelineEntity.getIdentifier()));
    }
  }

  @Override
  public void saveExecutionInfo(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    Criteria criteria =
        PMSPipelineServiceHelper.getPipelineEqualityCriteria(accountId, orgId, projectId, pipelineId, false, null);

    Update update = new Update();
    update.set(PipelineEntityKeys.executionSummaryInfo, executionSummaryInfo);
    pmsPipelineRepository.update(criteria, update);
  }

  @Override
  public Optional<PipelineEntity> incrementRunSequence(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean deleted) {
    return pmsPipelineRepository.incrementRunSequence(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, deleted);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    Criteria criteria = PMSPipelineServiceHelper.getPipelineEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false, version);

    UpdateResult updateResult = pmsPipelineRepository.delete(criteria);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(
          format("Pipeline [%s] under Project[%s], Organization [%s] couldn't be deleted.", pipelineIdentifier,
              projectIdentifier, orgIdentifier));
    }

    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    pipelineEntity.ifPresent(entity -> pipelineSubject.fireInform(PipelineActionObxxxxxxxx:onDelete, entity));
    return true;
  }

  @Override
  public Page<PipelineEntity> list(Criteria criteria, Pageable pageable) {
    return pmsPipelineRepository.findAll(criteria, pageable);
  }

  @Override
  public StepCategory getSteps(String module, String category, String accountId) {
    Map<String, List<StepInfo>> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getInstanceNameToSupportedSteps();
    StepCategory stepCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
        category, serviceInstanceNameToSupportedSteps.get(module), accountId);
    for (Map.Entry<String, List<StepInfo>> entry : serviceInstanceNameToSupportedSteps.entrySet()) {
      if (entry.getKey().equals(module) || EmptyPredicate.isEmpty(entry.getValue())) {
        continue;
      }
      stepCategory.addStepCategory(
          PMSPipelineServiceStepHelper.calculateStepsForCategory(entry.getKey(), entry.getValue()));
    }
    return stepCategory;
  }

  @Override
  public VariableMergeServiceResponse createVariablesResponse(PipelineEntity pipelineEntity) {
    try {
      return variableCreatorMergeService.createVariablesResponse(pipelineEntity.getYaml());
    } catch (Exception ex) {
      throw new InvalidRequestException(
          format("Error happened while creating variables for pipeline: %s", ex.getMessage()));
    }
  }

  @Override
  public Criteria formCriteria(String accountId, String orgId, String projectId, String filterIdentifier,
      PipelineFilterPropertiesDto filterProperties, boolean deleted, String module, String searchTerm) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(PipelineEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgId)) {
      criteria.and(PipelineEntityKeys.orgIdentifier).is(orgId);
    }
    if (isNotEmpty(projectId)) {
      criteria.and(PipelineEntityKeys.projectIdentifier).is(projectId);
    }
    criteria.and(PipelineEntityKeys.deleted).is(deleted);

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      pmsPipelineServiceHelper.populateFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      PMSPipelineServiceHelper.populateFilter(criteria, filterProperties);
    }
    if (EmptyPredicate.isNotEmpty(module)) {
      // Check for pipeline with no filters also - empty pipeline or pipelines with only approval stage
      // criteria = { "$or": [ { "filters": {} } , { "filters.MODULE": { $exists: true } } ] }
      Criteria moduleCriteria = new Criteria().orOperator(where(PipelineEntityKeys.filters).is(new Document()),
          where(String.format("%s.%s", PipelineEntityKeys.filters, module)).exists(true));
      criteria.andOperator(moduleCriteria);
    }

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(PipelineEntityKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.tags + "." + NGTagKeys.key)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.tags + "." + NGTagKeys.value)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }

    return criteria;
  }
}
