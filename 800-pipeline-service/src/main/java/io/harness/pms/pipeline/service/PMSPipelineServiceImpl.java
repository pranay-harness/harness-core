package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import static io.harness.pms.pipeline.service.PMSPipelineServiceStepHelper.LIBRARY;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.ScmException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.observer.Subject;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepPalleteFilterWrapper;
import io.harness.pms.pipeline.StepPalleteInfo;
import io.harness.pms.pipeline.StepPalleteModuleInfo;
import io.harness.pms.pipeline.mappers.PipelineYamlDtoMapper;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.variables.VariableCreatorMergeService;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.pipeline.PMSPipelineRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  @Inject private GitSyncSdkService gitSyncSdkService;
  @Inject @Getter private final Subject<PipelineActionObserver> pipelineSubject = new Subject<>();
  @Inject private CommonStepInfo commonStepInfo;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
          pipelineEntity.getIdentifier());

      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);

      return pmsPipelineRepository.save(entityWithUpdatedInfo, PipelineYamlDtoMapper.toDto(entityWithUpdatedInfo));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format(DUP_KEY_EXP_FORMAT_STRING, pipelineEntity.getIdentifier(),
                                            pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);

    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);

    } catch (ExplanationException | ScmException e) {
      log.error("Error while updating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while saving pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while saving pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public Optional<PipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    try {
      return pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
          accountId, orgIdentifier, projectIdentifier, identifier, !deleted);
    } catch (Exception e) {
      log.error(String.format("Error while retrieving pipeline [%s]", identifier), e);
      throw new InvalidRequestException(
          String.format("Error while retrieving pipeline [%s]: %s", identifier, ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineEntity updatePipelineYaml(PipelineEntity pipelineEntity, ChangeType changeType) {
    PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());

    if (GitContextHelper.getGitEntityInfo() != null && GitContextHelper.getGitEntityInfo().isNewBranch()) {
      // sending old entity as null here because a new mongo entity will be created. If audit trail needs to be added
      // to git synced projects, a get call needs to be added here to the base branch of this pipeline update
      return makePipelineUpdateCall(pipelineEntity, null, changeType);
    }
    Optional<PipelineEntity> optionalOriginalEntity =
        pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
            pipelineEntity.getIdentifier(), true);
    if (!optionalOriginalEntity.isPresent()) {
      throw new InvalidRequestException(format("Pipeline [%s] under Project[%s], Organization [%s] doesn't exist.",
          pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
    }
    PipelineEntity entityToUpdate = optionalOriginalEntity.get();
    PipelineEntity tempEntity = entityToUpdate.withYaml(pipelineEntity.getYaml())
                                    .withName(pipelineEntity.getName())
                                    .withDescription(pipelineEntity.getDescription())
                                    .withTags(pipelineEntity.getTags());

    return makePipelineUpdateCall(tempEntity, entityToUpdate, changeType);
  }

  private PipelineEntity makePipelineUpdateCall(
      PipelineEntity pipelineEntity, PipelineEntity oldEntity, ChangeType changeType) {
    try {
      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);
      PipelineEntity updatedResult = pmsPipelineRepository.updatePipelineYaml(
          entityWithUpdatedInfo, oldEntity, PipelineYamlDtoMapper.toDto(entityWithUpdatedInfo), changeType);

      if (updatedResult == null) {
        throw new InvalidRequestException(format(
            "Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
            pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
      }

      pipelineSubject.fireInform(PipelineActionObxxxxxxxx:onUpdate, updatedResult);

      return updatedResult;
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    } catch (ExplanationException | ScmException e) {
      log.error("Error while updating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while updating pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while updating pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update updateOperations) {
    return pmsPipelineRepository.updatePipelineMetadata(
        accountId, orgIdentifier, projectIdentifier, criteria, updateOperations);
  }

  @Override
  public void saveExecutionInfo(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    Criteria criteria =
        PMSPipelineServiceHelper.getPipelineEqualityCriteria(accountId, orgId, projectId, pipelineId, false, null);

    Update update = new Update();
    update.set(PipelineEntityKeys.executionSummaryInfo, executionSummaryInfo);
    updatePipelineMetadata(accountId, orgId, projectId, criteria, update);
  }

  @Override
  public Optional<PipelineEntity> incrementRunSequence(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean deleted) {
    Criteria criteria = PMSPipelineServiceHelper.getPipelineEqualityCriteria(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false, null);
    Update update = new Update();
    update.inc(PipelineEntityKeys.runSequence);
    return Optional.ofNullable(updatePipelineMetadata(accountId, orgIdentifier, projectIdentifier, criteria, update));
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    Optional<PipelineEntity> optionalPipelineEntity =
        get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(format("Pipeline [%s] under Project[%s], Organization [%s] does not exist.",
          pipelineIdentifier, projectIdentifier, orgIdentifier));
    }

    PipelineEntity existingEntity = optionalPipelineEntity.get();
    PipelineEntity withDeleted = existingEntity.withDeleted(true);
    try {
      PipelineEntity deletedEntity =
          pmsPipelineRepository.deletePipeline(withDeleted, PipelineYamlDtoMapper.toDto(withDeleted));
      if (deletedEntity.getDeleted()) {
        pipelineSubject.fireInform(PipelineActionObxxxxxxxx:onDelete, deletedEntity);
        return true;
      } else {
        throw new InvalidRequestException(
            format("Pipeline [%s] under Project[%s], Organization [%s] could not be deleted.", pipelineIdentifier,
                projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting pipeline [%s]", pipelineIdentifier), e);
      throw new InvalidRequestException(
          String.format("Error while deleting pipeline [%s]: %s", pipelineIdentifier, ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public Page<PipelineEntity> list(Criteria criteria, Pageable pageable, String accountId, String orgIdentifier,
      String projectIdentifier, Boolean getDistinctFromBranches) {
    if (Boolean.TRUE.equals(getDistinctFromBranches)
        && gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, true);
    }
    return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, false);
  }

  @Override
  public StepCategory getSteps(String module, String category, String accountId) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    StepCategory stepCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
        category, serviceInstanceNameToSupportedSteps.get(module).getStepTypes(), accountId);
    for (Map.Entry<String, StepPalleteInfo> entry : serviceInstanceNameToSupportedSteps.entrySet()) {
      if (entry.getKey().equals(module) || EmptyPredicate.isEmpty(entry.getValue().getStepTypes())) {
        continue;
      }
      stepCategory.addStepCategory(pmsPipelineServiceStepHelper.calculateStepsForCategory(
          entry.getValue().getModuleName(), entry.getValue().getStepTypes(), accountId));
    }
    return stepCategory;
  }

  @Override
  public StepCategory getStepsV2(String accountId, StepPalleteFilterWrapper stepPalleteFilterWrapper) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    if (stepPalleteFilterWrapper.getStepPalleteModuleInfos().isEmpty()) {
      // Return all the steps.
      return pmsPipelineServiceStepHelper.getAllSteps(accountId, serviceInstanceNameToSupportedSteps);
    }
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (StepPalleteModuleInfo request : stepPalleteFilterWrapper.getStepPalleteModuleInfos()) {
      String module = request.getModule();
      String category = request.getCategory();
      StepPalleteInfo stepPalleteInfo = serviceInstanceNameToSupportedSteps.get(module);
      if (stepPalleteInfo == null) {
        continue;
      }
      List<StepInfo> stepInfoList = stepPalleteInfo.getStepTypes();
      String displayModuleName = stepPalleteInfo.getModuleName();
      if (EmptyPredicate.isEmpty(stepInfoList)) {
        continue;
      }
      StepCategory moduleCategory;
      if (EmptyPredicate.isNotEmpty(category)) {
        moduleCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategoryV2(
            displayModuleName, category, stepInfoList, accountId);
      } else {
        moduleCategory =
            pmsPipelineServiceStepHelper.calculateStepsForCategory(displayModuleName, stepInfoList, accountId);
      }
      stepCategory.addStepCategory(moduleCategory);
      if (request.isShouldShowCommonSteps()) {
        pmsPipelineServiceStepHelper.addStepsToStepCategory(
            moduleCategory, commonStepInfo.getCommonSteps(request.getCommonStepCategory()), accountId);
      }
    }

    return stepCategory;
  }

  @Override
  public VariableMergeServiceResponse createVariablesResponse(String yaml) {
    try {
      return variableCreatorMergeService.createVariablesResponse(yaml);
    } catch (Exception ex) {
      log.error("Error happened while creating variables for pipeline:", ex);
      throw new InvalidRequestException(
          format("Error happened while creating variables for pipeline: %s", ex.getMessage()));
    }
  }

  // Todo: Remove only if there are no references to the pipeline
  @Override
  public boolean deleteAllPipelinesInAProject(String accountId, String orgId, String projectId) {
    Criteria criteria = formCriteria(
        accountId, orgId, projectId, null, PipelineFilterPropertiesDto.builder().build(), false, null, null);
    Pageable pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));

    Page<PipelineEntity> pipelineEntities =
        pmsPipelineRepository.findAll(criteria, pageRequest, accountId, orgId, projectId, false);
    for (PipelineEntity pipelineEntity : pipelineEntities) {
      pmsPipelineRepository.deletePipeline(
          pipelineEntity.withDeleted(true), PipelineYamlDtoMapper.toDto(pipelineEntity.withDeleted(true)));
    }
    return true;
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

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      pmsPipelineServiceHelper.populateFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      PMSPipelineServiceHelper.populateFilter(criteria, filterProperties);
    }

    Criteria moduleCriteria = new Criteria();
    if (EmptyPredicate.isNotEmpty(module)) {
      // Check for pipeline with no filters also - empty pipeline or pipelines with only approval stage
      // criteria = { "$or": [ { "filters": {} } , { "filters.MODULE": { $exists: true } } ] }
      moduleCriteria.orOperator(where(PipelineEntityKeys.filters).is(new Document()),
          where(String.format("%s.%s", PipelineEntityKeys.filters, module)).exists(true));
    }

    Criteria searchCriteria = new Criteria();
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      searchCriteria.orOperator(where(PipelineEntityKeys.identifier)
                                    .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.tags + "." + NGTagKeys.key)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          where(PipelineEntityKeys.tags + "." + NGTagKeys.value)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
    }

    criteria.andOperator(moduleCriteria, searchCriteria);

    return criteria;
  }
}
