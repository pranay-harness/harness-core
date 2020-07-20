package io.harness.cdng.pipeline.service;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.cdng.pipeline.repository.PipelineRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
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
      CDPipelineEntity cdPipelineEntity = CDPipelineEntity.builder()
                                              .cdPipeline(cdPipeline)
                                              .yamlPipeline(yaml)
                                              .accountId(accountId)
                                              .orgIdentifier(orgId)
                                              .projectIdentifier(projectId)
                                              .identifier(cdPipeline.getIdentifier())
                                              .build();
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
  public String updatePipeline(String yaml, String accountId, String orgId, String projectId) {
    try {
      CDPipeline cdPipeline = YamlPipelineUtils.read(yaml, CDPipeline.class);
      CDPipelineEntity cdPipelineEntity = CDPipelineEntity.builder()
                                              .cdPipeline(cdPipeline)
                                              .yamlPipeline(yaml)
                                              .accountId(accountId)
                                              .orgIdentifier(orgId)
                                              .projectIdentifier(projectId)
                                              .identifier(cdPipeline.getIdentifier())
                                              .build();
      Optional<CDPipelineEntity> cdPipelineEntityExisting =
          pipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
              accountId, orgId, projectId, cdPipeline.getIdentifier());
      if (cdPipelineEntityExisting.isPresent()) {
        cdPipelineEntity.setUuid(cdPipelineEntityExisting.get().getUuid());
        CDPipelineEntity savedCdPipeline = pipelineRepository.save(cdPipelineEntity);
        return savedCdPipeline.getIdentifier();
      } else {
        throw new GeneralException(String.format("Pipeline with Id: %s does not exist.", cdPipeline.getIdentifier()));
      }
    } catch (IOException e) {
      throw new GeneralException("error while de-serializing pipeline. is this valid Yaml?", e);
    } catch (NoSuchElementException ex) {
      throw new GeneralException("Pipeline does not exist", ex);
    }
  }

  @Override
  public CDPipelineDTO createPipelineReturnYaml(String yaml, String accountId, String orgId, String projectId) {
    try {
      CDPipeline cdPipeline = YamlPipelineUtils.read(yaml, CDPipeline.class);
      CDPipelineEntity cdPipelineEntity = CDPipelineEntity.builder()
                                              .cdPipeline(cdPipeline)
                                              .yamlPipeline(yaml)
                                              .accountId(accountId)
                                              .orgIdentifier(orgId)
                                              .projectIdentifier(projectId)
                                              .identifier(cdPipeline.getIdentifier())
                                              .build();
      CDPipelineEntity savedCdPipeline = pipelineRepository.save(cdPipelineEntity);
      return CDPipelineDTO.builder()
          .name(savedCdPipeline.getCdPipeline().getName())
          .description(savedCdPipeline.getCdPipeline().getDescription())
          .identifier(savedCdPipeline.getIdentifier())
          .stages(savedCdPipeline.getCdPipeline().getStages())
          .build();
    } catch (IOException e) {
      throw new GeneralException("error while saving pipeline", e);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Pipeline already exists under project %s", projectId), USER_SRE, ex);
    }
  }

  @Override
  public CDPipelineDTO getPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    Optional<CDPipelineEntity> cdPipeline =
        pipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountId, orgId, projectId, pipelineId);
    if (cdPipeline.isPresent()) {
      return CDPipelineDTO.builder()
          .name(cdPipeline.get().getCdPipeline().getName())
          .description(cdPipeline.get().getCdPipeline().getDescription())
          .identifier(cdPipeline.get().getCdPipeline().getIdentifier())
          .stages(cdPipeline.get().getCdPipeline().getStages())
          .yamlPipeline(cdPipeline.get().getYamlPipeline())
          .build();
    } else {
      throw new GeneralException(String.format("Pipeline with ID [%s] Not found", pipelineId));
    }
  }

  @Override
  public Page<CDPipelineDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable) {
    // TODO: Remove usage of mongotemplate from here and move to repository
    criteria = criteria.and(CDPipelineEntity.PipelineNGKeys.accountId)
                   .is(accountId)
                   .and(CDPipelineEntity.PipelineNGKeys.projectIdentifier)
                   .is(projectId)
                   .and(CDPipelineEntity.PipelineNGKeys.orgIdentifier)
                   .is(orgId);
    Page<CDPipelineEntity> list = pipelineRepository.findAll(criteria, pageable);
    return list.map(cdPipelineEntity
        -> CDPipelineDTO.builder()
               .name(cdPipelineEntity.getCdPipeline().getName())
               .description(cdPipelineEntity.getCdPipeline().getDescription())
               .identifier(cdPipelineEntity.getCdPipeline().getIdentifier())
               .stages(cdPipelineEntity.getCdPipeline().getStages())
               .yamlPipeline(cdPipelineEntity.getYamlPipeline())
               .build());
  }
}
