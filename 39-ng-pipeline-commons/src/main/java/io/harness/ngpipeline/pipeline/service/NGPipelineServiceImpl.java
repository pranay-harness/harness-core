package io.harness.ngpipeline.pipeline.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.UpdateResult;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;
import io.harness.ngpipeline.pipeline.repository.spring.NgPipelineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Singleton
@Slf4j
public class NGPipelineServiceImpl implements NGPipelineService {
  @Inject private NgPipelineRepository ngPipelineRepository;
  @Inject private InputSetEntityService inputSetEntityService;
  @Inject @Named("NgPipelineCommonsExecutor") private ExecutorService executorService;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public NgPipelineEntity create(NgPipelineEntity ngPipeline) {
    try {
      validatePresenceOfRequiredFields(ngPipeline.getAccountId(), ngPipeline.getOrgIdentifier(),
          ngPipeline.getProjectIdentifier(), ngPipeline.getIdentifier(), ngPipeline.getIdentifier());
      return ngPipelineRepository.save(ngPipeline);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format(DUP_KEY_EXP_FORMAT_STRING, ngPipeline.getIdentifier(),
                                            ngPipeline.getProjectIdentifier(), ngPipeline.getOrgIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<NgPipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    return ngPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, identifier, !deleted);
  }

  @Override
  public NgPipelineEntity update(NgPipelineEntity ngPipeline) {
    validatePresenceOfRequiredFields(ngPipeline.getAccountId(), ngPipeline.getOrgIdentifier(),
        ngPipeline.getProjectIdentifier(), ngPipeline.getIdentifier());
    Criteria criteria = getPipelineEqualityCriteria(ngPipeline, ngPipeline.getDeleted());
    NgPipelineEntity updateResult = ngPipelineRepository.update(criteria, ngPipeline);
    if (updateResult == null) {
      throw new InvalidRequestException(
          String.format("Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
              ngPipeline.getIdentifier(), ngPipeline.getProjectIdentifier(), ngPipeline.getOrgIdentifier()));
    }
    return updateResult;
  }

  @Override
  public Page<NgPipelineEntity> list(Criteria criteria, Pageable pageable) {
    return ngPipelineRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    executorService.submit(() -> {
      inputSetEntityService.deleteInputSetsOfPipeline(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    });
    Criteria criteria =
        getPipelineEqualityCriteria(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false, version);
    UpdateResult updateResult = ngPipelineRepository.delete(criteria);
    if (!updateResult.wasAcknowledged() || updateResult.getModifiedCount() != 1) {
      throw new InvalidRequestException(
          String.format("Pipeline [%s] under Project[%s], Organization [%s] couldn't be deleted.", pipelineIdentifier,
              projectIdentifier, orgIdentifier));
    }
    return true;
  }

  private Criteria getPipelineEqualityCriteria(@Valid NgPipelineEntity requestPipeline, boolean deleted) {
    return getPipelineEqualityCriteria(requestPipeline.getAccountId(), requestPipeline.getOrgIdentifier(),
        requestPipeline.getProjectIdentifier(), requestPipeline.getIdentifier(), deleted, requestPipeline.getVersion());
  }

  private Criteria getPipelineEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean deleted, Long version) {
    Criteria criteria = getPipelineEqualityCriteria(accountId, orgIdentifier, projectIdentifier, version);
    return criteria.and(PipelineNGKeys.identifier).is(pipelineIdentifier).and(PipelineNGKeys.deleted).is(deleted);
  }

  private Criteria getPipelineEqualityCriteria(
      String accountId, String orgIdentifier, String projectIdentifier, Long version) {
    Criteria criteria = Criteria.where(PipelineNGKeys.accountId)
                            .is(accountId)
                            .and(PipelineNGKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(PipelineNGKeys.projectIdentifier)
                            .is(projectIdentifier);

    if (version != null) {
      criteria.and(PipelineNGKeys.version).is(version);
    }

    return criteria;
  }

  @Override
  public Map<String, String> getPipelineIdentifierToName(
      String accountId, String orgId, String projectId, List<String> pipelineIdentifiers) {
    Criteria criteria = getPipelineEqualityCriteria(accountId, orgId, projectId, null);
    criteria.and(PipelineNGKeys.identifier).in(pipelineIdentifiers);
    return ngPipelineRepository
        .findAllWithCriteriaAndProjectOnFields(criteria,
            Lists.newArrayList(PipelineNGKeys.ngPipeline + ".name", PipelineNGKeys.identifier, PipelineNGKeys.createdAt,
                PipelineNGKeys.lastUpdatedAt),
            new ArrayList<>())
        .stream()
        .collect(Collectors.toMap(
            NgPipelineEntity::getIdentifier, ngPipelineEntity -> ngPipelineEntity.getNgPipeline().getName()));
  }

  @Override
  public NgPipelineEntity getPipeline(String uuid) {
    // TODO Validate accountId and fix read pipeline code
    return ngPipelineRepository.findById(uuid).orElseThrow(
        () -> new IllegalArgumentException(format("Pipeline id:%s not found", uuid)));
  }

  @Override
  public NgPipelineEntity getPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    return get(accountId, orgId, projectId, pipelineId, false)
        .orElseThrow(() -> new InvalidRequestException(format("Pipeline id:%s not found", pipelineId)));
  }

  @Override
  public Page<NgPipelineEntity> listPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm) {
    criteria = criteria.and(PipelineNGKeys.deleted).is(false);
    criteria = criteria.andOperator(getPipelineEqualityCriteria(accountId, orgId, projectId, null));
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(PipelineNGKeys.identifier).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
      // add name and tags in search when they are added to the entity
    }

    return ngPipelineRepository.findAll(criteria, pageable);
  }
}
