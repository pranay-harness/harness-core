package io.harness.cdng.pipeline.service;

import static io.harness.exception.WingsException.USER_SRE;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.NGResourceFilterConstants;
import io.harness.beans.ExecutionStrategyType;
import io.harness.cdng.pipeline.NGStepType;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.pipeline.StepData;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;
import io.harness.cdng.pipeline.mappers.PipelineDtoMapper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.PipelineDoesNotExistException;
import io.harness.ngpipeline.pipeline.repository.PipelineRepository;
import io.harness.walktree.visitor.response.VisitorErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PipelineServiceImpl implements PipelineService {
  @VisibleForTesting static String LIBRARY = "Library";

  @Inject PipelineRepository pipelineRepository;
  private LoadingCache<ServiceDefinitionType, StepCategory> stepsCache =
      CacheBuilder.newBuilder().build(new CacheLoader<ServiceDefinitionType, StepCategory>() {
        @Override
        public StepCategory load(final ServiceDefinitionType serviceDefinitionType) throws Exception {
          return calculateStepsForServiceDefinitionType(serviceDefinitionType);
        }
      });

  @Override
  public String createPipeline(String yaml, String accountId, String orgId, String projectId) {
    try {
      NgPipeline ngPipeline = YamlPipelineUtils.read(yaml, NgPipeline.class);
      NgPipelineEntity ngPipelineEntity =
          PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml, ngPipeline);
      NgPipelineEntity savedNgPipelineEntity = pipelineRepository.save(ngPipelineEntity);
      return savedNgPipelineEntity.getIdentifier();
    } catch (IOException e) {
      throw new GeneralException("Error while de-serializing pipeline. is this valid Yaml? - " + e.getMessage(), e);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Pipeline already exists under project %s ", projectId), USER_SRE, ex);
    }
  }

  @Override
  public String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId) {
    try {
      NgPipelineEntity ngPipelineEntityExisting = get(pipelineId, accountId, orgId, projectId);
      NgPipeline ngPipeline = YamlPipelineUtils.read(yaml, NgPipeline.class);
      if (pipelineId.equalsIgnoreCase(ngPipeline.getIdentifier())) {
        NgPipelineEntity ngPipelineEntity =
            PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml, ngPipeline);
        ngPipelineEntity.setUuid(ngPipelineEntityExisting.getUuid());
        NgPipelineEntity savedNgPipelineEntity = pipelineRepository.save(ngPipelineEntity);
        return savedNgPipelineEntity.getIdentifier();
      } else {
        throw new InvalidRequestException(
            "Pipeline Identifier in the query Param & Identifier in the pipeline yaml are not matching");
      }
    } catch (IOException e) {
      throw new GeneralException("Error while de-serializing pipeline. is this valid Yaml? - " + e.getMessage(), e);
    } catch (NoSuchElementException ex) {
      throw new GeneralException("Pipeline does not exist", ex);
    }
  }

  @Override
  public Optional<CDPipelineResponseDTO> getPipeline(
      String pipelineId, String accountId, String orgId, String projectId) {
    NgPipelineEntity pipelineEntity = get(pipelineId, accountId, orgId, projectId);
    return Optional.of(pipelineEntity).map(PipelineDtoMapper::writePipelineDto);
  }

  @Override
  public Page<CDPipelineSummaryResponseDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm) {
    // TODO: Remove usage of mongotemplate from here and move to repository
    criteria = criteria.and(PipelineNGKeys.accountId)
                   .is(accountId)
                   .and(PipelineNGKeys.projectIdentifier)
                   .is(projectId)
                   .and(PipelineNGKeys.orgIdentifier)
                   .is(orgId)
                   .and(PipelineNGKeys.deleted)
                   .is(false);
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(PipelineNGKeys.identifier).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
      // add name and tags in search when they are added to the entity
    }

    Page<NgPipelineEntity> list = pipelineRepository.findAll(criteria, pageable);
    return list.map(PipelineDtoMapper::preparePipelineSummary);
  }

  @Override
  public String getExecutionStrategyYaml(
      ServiceDefinitionType serviceDefinitionType, ExecutionStrategyType executionStrategyType) throws IOException {
    if (ServiceDefinitionType.getExecutionStrategies(serviceDefinitionType).contains(executionStrategyType)) {
      ClassLoader classLoader = this.getClass().getClassLoader();
      return Resources.toString(
          Objects.requireNonNull(classLoader.getResource(
              String.format("executionStrategyYaml/%s-%s.yaml", serviceDefinitionType.getYamlName().toLowerCase(),
                  executionStrategyType.getDisplayName().toLowerCase()))),
          StandardCharsets.UTF_8);
    } else {
      throw new GeneralException("Execution Strategy Not supported for given deployment type");
    }
  }

  @Override

  public List<ServiceDefinitionType> getServiceDefinitionTypes() {
    return Arrays.asList(ServiceDefinitionType.values());
  }

  public StepCategory getSteps(ServiceDefinitionType serviceDefinitionType) {
    try {
      return stepsCache.get(serviceDefinitionType);
    } catch (ExecutionException e) {
      throw new GeneralException("Exception occured while calculating the list of steps");
    }
  }

  private StepCategory calculateStepsForServiceDefinitionType(ServiceDefinitionType serviceDefinitionType) {
    List<NGStepType> filteredNGStepTypes =
        Arrays.stream(NGStepType.values())
            .filter(ngStepType -> NGStepType.getServiceDefinitionTypes(ngStepType).contains(serviceDefinitionType))
            .collect(Collectors.toList());
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (NGStepType stepType : filteredNGStepTypes) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  private void addToTopLevel(StepCategory stepCategory, NGStepType stepType) {
    String categories = NGStepType.getCategory(stepType);
    String[] categoryArrayName = categories.split("/");
    StepCategory currentStepCategory = stepCategory;
    for (String catogoryName : categoryArrayName) {
      currentStepCategory = currentStepCategory.getOrCreateChildStepCategory(catogoryName);
    }
    currentStepCategory.addStepData(
        StepData.builder().name(NGStepType.getDisplayName(stepType)).type(stepType).build());
  }

  @Override
  public Map<ServiceDefinitionType, List<ExecutionStrategyType>> getExecutionStrategyList() {
    return Arrays.stream(ServiceDefinitionType.values())
        .collect(Collectors.toMap(
            serviceDefinitionType -> serviceDefinitionType, ServiceDefinitionType::getExecutionStrategies));
  }

  @Override
  public boolean deletePipeline(String accountId, String orgId, String projectId, String pipelineId) {
    try {
      NgPipelineEntity ngPipelineEntity = get(pipelineId, accountId, orgId, projectId);
      ngPipelineEntity.setDeleted(true);
      pipelineRepository.save(ngPipelineEntity);
    } catch (PipelineDoesNotExistException e) {
      // ignore exception
    }
    return true;
  }

  /**
   * Todo: Proper implementation will be after merging validation framework.
   * @param pipelineId
   * @param accountId
   * @param orgId
   * @param projectId
   * @return
   */
  @Override
  public Optional<CDPipelineValidationInfo> validatePipeline(
      String pipelineId, String accountId, String orgId, String projectId) {
    Map<String, VisitorErrorResponseWrapper> uuidToErrorResponse = new HashMap<>();
    VisitorErrorResponse errorResponse =
        VisitorErrorResponse.errorBuilder().fieldName("identifier").message("cannot be null").build();
    uuidToErrorResponse.put(
        "pipeline.identifier", VisitorErrorResponseWrapper.builder().errors(Lists.newArrayList(errorResponse)).build());
    uuidToErrorResponse.put("pipeline.stage.identifier",
        VisitorErrorResponseWrapper.builder().errors(Lists.newArrayList(errorResponse)).build());
    NgPipeline ngPipeline =
        NgPipeline.builder()
            .identifier("pipeline.identifier")
            .name("dummyPipeline")
            .stage(StageElement.builder().name("dummyStage").identifier("pipeline.stage.identifier").build())
            .build();
    CDPipelineValidationInfo cdPipelineValidationInfo = CDPipelineValidationInfo.builder()
                                                            .uuidToValidationErrors(uuidToErrorResponse)
                                                            .ngPipeline(ngPipeline)
                                                            .isError(true)
                                                            .build();
    return Optional.of(cdPipelineValidationInfo);
  }

  private NgPipelineEntity get(String pipelineId, String accountId, String orgId, String projectId) {
    Optional<NgPipelineEntity> pipelineEntity =
        pipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgId, projectId, pipelineId, true);
    if (pipelineEntity.isPresent()) {
      return pipelineEntity.get();
    } else {
      throw new PipelineDoesNotExistException(pipelineId);
    }
  }
}
