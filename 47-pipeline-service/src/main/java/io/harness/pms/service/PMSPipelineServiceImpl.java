package io.harness.pms.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.beans.entities.PipelineEntity;
import io.harness.pms.beans.entities.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.beans.filters.FilterCreatorMergeServiceResponse;
import io.harness.pms.creator.FilterCreatorMergeService;
import io.harness.repositories.spring.PMSPipelineRepository;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class PMSPipelineServiceImpl implements PMSPipelineService {
  @Inject private PMSPipelineRepository pmsPipelineRepository;
  @Inject private FilterCreatorMergeService filterCreatorMergeService;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  // ToDo Need to add support for referred entities
  // saveReferencesPresentInPipeline

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      validatePresenceOfRequiredFields(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
          pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), pipelineEntity.getIdentifier());

      // Todo: Uncomment when we have the CD Service integration with NextGenApp.
      // updateFiltersAndStageCount(pipelineEntity);

      PipelineEntity createdEntity = pmsPipelineRepository.save(pipelineEntity);
      return createdEntity;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format(DUP_KEY_EXP_FORMAT_STRING, pipelineEntity.getIdentifier(),
                                            pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()),
          USER_SRE, ex);
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
    validatePresenceOfRequiredFields(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());

    Criteria criteria = getPipelineEqualityCriteria(pipelineEntity, pipelineEntity.getDeleted());
    PipelineEntity updateResult = pmsPipelineRepository.update(criteria, pipelineEntity);
    if (updateResult == null) {
      throw new InvalidRequestException(format(
          "Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
          pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
    }

    return updateResult;
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
    return true;
  }

  @Override
  public Page<PipelineEntity> list(Criteria criteria, Pageable pageable) {
    return pmsPipelineRepository.findAll(criteria, pageable);
  }

  private void updateFiltersAndStageCount(PipelineEntity pipelineEntity) {
    try {
      FilterCreatorMergeServiceResponse filtersAndStageCount =
          filterCreatorMergeService.getFiltersAndStageCount(pipelineEntity.getYaml());
      pipelineEntity.setStageCount(filtersAndStageCount.getStageCount());
      if (isNotEmpty(filtersAndStageCount.getFilters())) {
        filtersAndStageCount.getFilters().forEach(
            (key, value) -> pipelineEntity.getFilters().put(key, Document.parse(value)));
      }
    } catch (Exception ex) {
      throw new InvalidRequestException(
          format("Error happened while creating filters for pipeline: %s", ex.getMessage(), ex));
    }
  }
}
