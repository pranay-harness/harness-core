package io.harness.cdng.pipeline.service;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.cdng.pipeline.mappers.PipelineDtoMapper;
import io.harness.cdng.pipeline.repository.PipelineRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.PipelineDoesNotExistException;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

public class PipelineServiceImpl implements PipelineService {
  @Inject PipelineRepository pipelineRepository;

  @Override
  public String createPipeline(String yaml, String accountId, String orgId, String projectId) {
    try {
      CDPipeline cdPipeline = YamlPipelineUtils.read(yaml, CDPipeline.class);
      CDPipelineEntity cdPipelineEntity =
          PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml, cdPipeline);
      CDPipelineEntity savedCdPipeline = pipelineRepository.save(cdPipelineEntity);
      return savedCdPipeline.getIdentifier();
    } catch (IOException e) {
      throw new GeneralException("error while saving pipeline", e);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Pipeline already exists under project %s ", projectId), USER_SRE, ex);
    }
  }

  @Override
  public String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId) {
    try {
      CDPipelineEntity cdPipelineEntityExisting = get(pipelineId, accountId, orgId, projectId);
      CDPipeline cdPipeline = YamlPipelineUtils.read(yaml, CDPipeline.class);
      if (pipelineId.equalsIgnoreCase(cdPipeline.getIdentifier())) {
        CDPipelineEntity cdPipelineEntity =
            PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml, cdPipeline);
        cdPipelineEntity.setUuid(cdPipelineEntityExisting.getUuid());
        CDPipelineEntity savedCdPipeline = pipelineRepository.save(cdPipelineEntity);
        return savedCdPipeline.getIdentifier();
      } else {
        throw new InvalidRequestException(
            "Pipeline Identifier in the query Param & Identifier in the pipeline yaml are not matching");
      }
    } catch (IOException e) {
      throw new GeneralException("error while de-serializing pipeline. is this valid Yaml?", e);
    } catch (NoSuchElementException ex) {
      throw new GeneralException("Pipeline does not exist", ex);
    }
  }

  @Override
  public Optional<CDPipelineResponseDTO> getPipeline(
      String pipelineId, String accountId, String orgId, String projectId) {
    CDPipelineEntity cdPipeline = get(pipelineId, accountId, orgId, projectId);
    return Optional.of(cdPipeline).map(PipelineDtoMapper::writePipelineDto);
  }

  @Override
  public Page<CDPipelineSummaryResponseDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable) {
    // TODO: Remove usage of mongotemplate from here and move to repository
    criteria = criteria.and(CDPipelineEntity.PipelineNGKeys.accountId)
                   .is(accountId)
                   .and(CDPipelineEntity.PipelineNGKeys.projectIdentifier)
                   .is(projectId)
                   .and(CDPipelineEntity.PipelineNGKeys.orgIdentifier)
                   .is(orgId)
                   .and(CDPipelineEntity.PipelineNGKeys.deleted)
                   .is(false);
    Page<CDPipelineEntity> list = pipelineRepository.findAll(criteria, pageable);
    return list.map(PipelineDtoMapper::preparePipelineSummary);
  }

  @Override
  public boolean deletePipeline(String accountId, String orgId, String projectId, String pipelineId) {
    try {
      CDPipelineEntity cdPipelineEntity = get(pipelineId, accountId, orgId, projectId);
      cdPipelineEntity.setDeleted(true);
      pipelineRepository.save(cdPipelineEntity);
    } catch (PipelineDoesNotExistException e) {
      // ignore exception
    }
    return true;
  }

  private CDPipelineEntity get(String pipelineId, String accountId, String orgId, String projectId) {
    Optional<CDPipelineEntity> cdPipelineEntity =
        pipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgId, projectId, pipelineId, true);
    if (cdPipelineEntity.isPresent()) {
      return cdPipelineEntity.get();
    } else {
      throw new PipelineDoesNotExistException(pipelineId);
    }
  }
}
