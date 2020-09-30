package io.harness.app.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.app.yaml.YAML;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.NgPipeline.NgPipelineKeys;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.repository.PipelineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Singleton
@Slf4j
public class CIPipelineServiceImpl implements CIPipelineService {
  @Inject private PipelineRepository ciPipelineRepository;
  @Inject private CIPipelineValidations ciPipelineValidations;
  @Inject private YAMLToObject yamlToObject;

  @Override
  public NgPipelineEntity createPipelineFromYAML(
      YAML yaml, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    NgPipeline ngPipeline = yamlToObject.convertYAML(yaml.getPipelineYAML());
    // TODO Move this to non yaml section or create new entity class
    NgPipelineEntity ngPipelineEntity = NgPipelineEntity.builder().ngPipeline(ngPipeline).build();
    ngPipelineEntity.setAccountId(accountIdentifier);
    ngPipelineEntity.setProjectIdentifier(projectIdentifier);
    ngPipelineEntity.setIdentifier(ngPipeline.getIdentifier());
    ngPipelineEntity.setOrgIdentifier(orgIdentifier);
    return ciPipelineRepository.save(ngPipelineEntity);
  }

  @Override
  public NgPipelineEntity createPipeline(NgPipelineEntity ngPipelineEntity) {
    ciPipelineValidations.validateCIPipeline(ngPipelineEntity);
    return ciPipelineRepository.save(ngPipelineEntity);
  }

  public NgPipelineEntity readPipeline(String pipelineId) {
    // TODO Validate accountId and fix read pipeline code
    return ciPipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public NgPipelineEntity readPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    return ciPipelineRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgId, projectId, pipelineId, true)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public List<NgPipelineEntity> getPipelines(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = createBuildFilterCriteria(ciPipelineFilterDTO);
    return ciPipelineRepository.findAllWithCriteria(criteria);
  }

  private Criteria createBuildFilterCriteria(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = Criteria.where(NgPipelineEntity.PipelineNGKeys.accountId)
                            .is(ciPipelineFilterDTO.getAccountIdentifier())
                            .and(NgPipelineEntity.PipelineNGKeys.orgIdentifier)
                            .is(ciPipelineFilterDTO.getOrgIdentifier())
                            .and(NgPipelineEntity.PipelineNGKeys.projectIdentifier)
                            .is(ciPipelineFilterDTO.getProjectIdentifier());

    if (isNotBlank(ciPipelineFilterDTO.getPipelineName())) {
      criteria.and(NgPipelineKeys.name).is(ciPipelineFilterDTO.getPipelineName());
    }
    if (isNotEmpty(ciPipelineFilterDTO.getTags())) {
      criteria.and(NgPipelineKeys.tags).in(ciPipelineFilterDTO.getTags());
    }
    return criteria;
  }
}
