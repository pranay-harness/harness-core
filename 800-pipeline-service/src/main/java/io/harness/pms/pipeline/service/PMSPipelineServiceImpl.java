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
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.observer.Subject;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.filter.creation.FilterCreatorMergeService;
import io.harness.pms.filter.creation.FilterCreatorMergeServiceResponse;
import io.harness.pms.filter.utils.ModuleInfoFilterUtils;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepData;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.variables.VariableCreatorMergeService;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.repositories.pipeline.PMSPipelineRepository;
import io.harness.serializer.JsonUtils;
import io.harness.service.GraphGenerationService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
  @Inject private FilterCreatorMergeService filterCreatorMergeService;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private VariableCreatorMergeService variableCreatorMergeService;
  @Inject private FilterService filterService;
  @Inject private PipelineSetupUsageHelper pipelineSetupUsageHelper;
  @Inject private CommonStepInfo commonStepInfo;
  @Inject @Getter private Subject<PipelineActionObserver> pipelineSubject = new Subject<>();

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";
  @VisibleForTesting static String LIBRARY = "Library";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      validatePresenceOfRequiredFields(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
          pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), pipelineEntity.getIdentifier());

      updatePipelineInfo(pipelineEntity);

      return pmsPipelineRepository.save(pipelineEntity);
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
      validatePresenceOfRequiredFields(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
          pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());

      updatePipelineInfo(pipelineEntity);

      Criteria criteria = getPipelineEqualityCriteria(pipelineEntity, pipelineEntity.getDeleted());
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

  private Criteria getPipelineEqualityCriteria(@Valid PipelineEntity requestPipeline, boolean deleted) {
    return getPipelineEqualityCriteria(requestPipeline.getAccountId(), requestPipeline.getOrgIdentifier(),
        requestPipeline.getProjectIdentifier(), requestPipeline.getIdentifier(), deleted, requestPipeline.getVersion());
  }

  private Criteria getPipelineEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean deleted, Long version) {
    Criteria criteria = getPipelineEqualityCriteria(accountId, orgIdentifier, projectIdentifier, version);
    return criteria.and(PipelineEntityKeys.identifier)
        .is(pipelineIdentifier)
        .and(PipelineEntityKeys.deleted)
        .is(deleted);
  }

  private Criteria getPipelineEqualityCriteria(
      String accountId, String orgIdentifier, String projectIdentifier, Long version) {
    Criteria criteria = Criteria.where(PipelineEntityKeys.accountId)
                            .is(accountId)
                            .and(PipelineEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(PipelineEntityKeys.projectIdentifier)
                            .is(projectIdentifier);

    if (version != null) {
      criteria.and(PipelineEntityKeys.version).is(version);
    }

    return criteria;
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    Criteria criteria =
        getPipelineEqualityCriteria(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false, version);

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

  private void updatePipelineInfo(PipelineEntity pipelineEntity) throws IOException, ProducerShutdownException {
    FilterCreatorMergeServiceResponse filtersAndStageCount = filterCreatorMergeService.getPipelineInfo(pipelineEntity);
    pipelineEntity.setStageCount(filtersAndStageCount.getStageCount());
    pipelineEntity.setStageNames(filtersAndStageCount.getStageNames());
    if (isNotEmpty(filtersAndStageCount.getFilters())) {
      filtersAndStageCount.getFilters().forEach(
          (key, value) -> pipelineEntity.getFilters().put(key, Document.parse(value)));
    }
  }

  @Override
  public VariableMergeServiceResponse createVariablesResponse(PipelineEntity pipelineEntity) {
    try {
      return variableCreatorMergeService.createVariablesResponse(pipelineEntity.getYaml());
    } catch (Exception ex) {
      throw new InvalidRequestException(
          format("Error happened while creating variables for pipeline: %s", ex.getMessage(), ex));
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
      populateFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      populateFilter(criteria, filterProperties);
    }
    if (EmptyPredicate.isNotEmpty(module)) {
      criteria.and(String.format("filters.%s", module)).exists(true);
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

  private void populateFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO = this.filterService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINESETUP);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a pipeline filter with the identifier ");
    } else {
      this.populateFilter(criteria, (PipelineFilterPropertiesDto) pipelineFilterDTO.getFilterProperties());
    }
  }

  private void populateFilter(Criteria criteria, @NotNull PipelineFilterPropertiesDto piplineFilter) {
    if (EmptyPredicate.isNotEmpty(piplineFilter.getName())) {
      criteria.and(PipelineEntityKeys.name).is(piplineFilter.getName());
    }
    if (EmptyPredicate.isNotEmpty(piplineFilter.getDescription())) {
      criteria.and(PipelineEntityKeys.description).is(piplineFilter.getDescription());
    }
    if (EmptyPredicate.isNotEmpty(piplineFilter.getPipelineTags())) {
      criteria.and(PipelineEntityKeys.tags).in(piplineFilter.getPipelineTags());
    }
    if (piplineFilter.getModuleProperties() != null) {
      ModuleInfoFilterUtils.processNode(
          JsonUtils.readTree(piplineFilter.getModuleProperties().toJson()), "filters", criteria);
    }
  }

  @Override
  public void saveExecutionInfo(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    Criteria criteria = getPipelineEqualityCriteria(accountId, orgId, projectId, null);
    criteria.and(PipelineEntityKeys.identifier).is(pipelineId);

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
  public StepCategory getSteps(String module, String category, String accountId) {
    Map<String, List<StepInfo>> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getInstanceNameToSupportedSteps();
    StepCategory stepCategory =
        calculateStepsForModuleBasedOnCategory(category, serviceInstanceNameToSupportedSteps.get(module), accountId);
    for (Map.Entry<String, List<StepInfo>> entry : serviceInstanceNameToSupportedSteps.entrySet()) {
      if (entry.getKey().equals(module) || EmptyPredicate.isEmpty(entry.getValue())) {
        continue;
      }
      stepCategory.addStepCategory(calculateStepsForCategory(entry.getKey(), entry.getValue()));
    }
    return stepCategory;
  }

  private StepCategory calculateStepsForCategory(String module, List<StepInfo> stepInfos) {
    StepCategory stepCategory = StepCategory.builder().name(module).build();
    for (StepInfo stepType : stepInfos) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  private StepCategory calculateStepsForModuleBasedOnCategory(
      String category, List<StepInfo> stepInfos, String accountId) {
    List<StepInfo> filteredStepTypes = new ArrayList<>();
    if (!stepInfos.isEmpty()) {
      filteredStepTypes =
          stepInfos.stream()
              .filter(stepInfo
                  -> EmptyPredicate.isEmpty(category) || stepInfo.getStepMetaData().getCategoryList().contains(category)
                      || EmptyPredicate.isEmpty(stepInfo.getStepMetaData().getCategoryList()))
              .collect(Collectors.toList());
    }
    filteredStepTypes.addAll(commonStepInfo.getCommonSteps(accountId));
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (StepInfo stepType : filteredStepTypes) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  private void addToTopLevel(StepCategory stepCategory, StepInfo stepInfo) {
    StepCategory currentStepCategory = stepCategory;
    if (stepInfo != null) {
      String folderPath = stepInfo.getStepMetaData().getFolderPath();
      String[] categoryArrayName = folderPath.split("/");
      for (String catogoryName : categoryArrayName) {
        currentStepCategory = currentStepCategory.getOrCreateChildStepCategory(catogoryName);
      }
      currentStepCategory.addStepData(StepData.builder().name(stepInfo.getName()).type(stepInfo.getType()).build());
    }
  }
}
