package io.harness.cdng.pipeline.service;

import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.NGResourceFilterConstants;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.dto.NGPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.NGPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;
import io.harness.cdng.pipeline.mappers.PipelineDtoMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.PipelineDoesNotExistException;
import io.harness.ngpipeline.pipeline.repository.PipelineRepository;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class NGPipelineServiceImpl implements NGPipelineService {
  @Inject PipelineRepository pipelineRepository;

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
      throw new DuplicateFieldException(format("Pipeline already exists under project %s ", projectId), USER_SRE, ex);
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
  public Optional<NGPipelineResponseDTO> getPipelineResponseDTO(
      String pipelineId, String accountId, String orgId, String projectId) {
    NgPipelineEntity pipelineEntity = get(pipelineId, accountId, orgId, projectId);
    return Optional.of(pipelineEntity).map(PipelineDtoMapper::writePipelineDto);
  }

  @Override
  public Page<NGPipelineSummaryResponseDTO> getPipelinesSummary(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm) {
    // TODO: Remove usage of mongotemplate from here and move to repository
    Page<NgPipelineEntity> list = listPipelines(accountId, orgId, projectId, criteria, pageable, searchTerm);
    return list.map(PipelineDtoMapper::preparePipelineSummary);
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

  @Override
  public Map<String, String> getPipelineIdentifierToName(
      String accountId, String orgId, String projectId, List<String> pipelineIdentifiers) {
    Criteria criteria = Criteria.where(PipelineNGKeys.accountId)
                            .is(accountId)
                            .and(PipelineNGKeys.projectIdentifier)
                            .is(projectId)
                            .and(PipelineNGKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineNGKeys.identifier)
                            .in(pipelineIdentifiers);
    return pipelineRepository
        .findAllWithCriteriaAndProjectOnFields(criteria,
            Lists.newArrayList(PipelineNGKeys.ngPipeline + ".name", PipelineNGKeys.identifier, PipelineNGKeys.createdAt,
                PipelineNGKeys.lastUpdatedAt),
            new ArrayList<>())
        .stream()
        .collect(Collectors.toMap(
            NgPipelineEntity::getIdentifier, ngPipelineEntity -> ngPipelineEntity.getNgPipeline().getName()));
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

  @Override
  public NgPipelineEntity getPipeline(String uuid) {
    // TODO Validate accountId and fix read pipeline code
    return pipelineRepository.findById(uuid).orElseThrow(
        () -> new IllegalArgumentException(format("Pipeline id:%s not found", uuid)));
  }

  @Override
  public NgPipelineEntity getPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    return pipelineRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgId, projectId, pipelineId, true)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  @Override
  public Page<NgPipelineEntity> listPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm) {
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

    return pipelineRepository.findAll(criteria, pageable);
  }
}
