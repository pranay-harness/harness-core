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
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.CDPipeline.CDPipelineKeys;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
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
  public CDPipelineEntity createPipelineFromYAML(
      YAML yaml, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    CDPipeline ciPipeline = yamlToObject.convertYAML(yaml.getPipelineYAML());
    // TODO Move this to non yaml section or create new entity class
    CDPipelineEntity cdPipelineEntity = CDPipelineEntity.builder().cdPipeline(ciPipeline).build();
    cdPipelineEntity.setAccountId(accountIdentifier);
    cdPipelineEntity.setProjectIdentifier(projectIdentifier);
    cdPipelineEntity.setIdentifier(ciPipeline.getIdentifier());
    cdPipelineEntity.setOrgIdentifier(orgIdentifier);
    return ciPipelineRepository.save(cdPipelineEntity);
  }

  @Override
  public CDPipelineEntity createPipeline(CDPipelineEntity ciPipeline) {
    ciPipelineValidations.validateCIPipeline(ciPipeline);
    return ciPipelineRepository.save(ciPipeline);
  }

  public CDPipelineEntity readPipeline(String pipelineId) {
    // TODO Validate accountId and fix read pipeline code
    return ciPipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public CDPipelineEntity readPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    return ciPipelineRepository
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgId, projectId, pipelineId, true)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public List<CDPipelineEntity> getPipelines(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = createBuildFilterCriteria(ciPipelineFilterDTO);
    return ciPipelineRepository.findAllWithCriteria(criteria);
  }

  private Criteria createBuildFilterCriteria(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = Criteria.where(CDPipelineEntity.PipelineNGKeys.accountId)
                            .is(ciPipelineFilterDTO.getAccountIdentifier())
                            .and(CDPipelineEntity.PipelineNGKeys.orgIdentifier)
                            .is(ciPipelineFilterDTO.getOrgIdentifier())
                            .and(CDPipelineEntity.PipelineNGKeys.projectIdentifier)
                            .is(ciPipelineFilterDTO.getProjectIdentifier());

    if (isNotBlank(ciPipelineFilterDTO.getPipelineName())) {
      criteria.and(CDPipelineKeys.name).is(ciPipelineFilterDTO.getPipelineName());
    }
    if (isNotEmpty(ciPipelineFilterDTO.getTags())) {
      criteria.and(CDPipelineKeys.tags).in(ciPipelineFilterDTO.getTags());
    }
    return criteria;
  }
}
