package io.harness.app.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.dao.repositories.CIPipelineRepository;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Singleton
@Slf4j
public class CIPipelineServiceImpl implements CIPipelineService {
  @Inject private CIPipelineRepository ciPipelineRepository;
  @Inject private CIPipelineValidations ciPipelineValidations;
  @Inject private YAMLToObject yamlToObject;

  @Override
  public CIPipeline createPipelineFromYAML(
      YAML yaml, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml.getPipelineYAML());
    // TODO Move this to non yaml section or create new entity class
    ciPipeline.setAccountId(accountIdentifier);
    ciPipeline.setProjectId(projectIdentifier);
    ciPipeline.setOrganizationId(orgIdentifier);
    return ciPipelineRepository.save(ciPipeline);
  }

  @Override
  public CIPipeline createPipeline(CIPipeline ciPipeline) {
    ciPipelineValidations.validateCIPipeline(ciPipeline);
    return ciPipelineRepository.save(ciPipeline);
  }

  public CIPipeline readPipeline(String pipelineId) {
    // TODO Validate accountId and fix read pipeline code
    return ciPipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public CIPipeline readPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    return ciPipelineRepository
        .findByAccountIdAndOrganizationIdAndProjectIdAndIdentifier(accountId, orgId, projectId, pipelineId)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }

  public List<CIPipeline> getPipelines(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = createBuildFilterCriteria(ciPipelineFilterDTO);
    return ciPipelineRepository.findAllWithCriteria(criteria);
  }

  private Criteria createBuildFilterCriteria(CIPipelineFilterDTO ciPipelineFilterDTO) {
    Criteria criteria = Criteria.where(CIPipeline.Pipeline.accountId)
                            .is(ciPipelineFilterDTO.getAccountIdentifier())
                            .and(CIPipeline.Pipeline.organizationId)
                            .is(ciPipelineFilterDTO.getOrgIdentifier())
                            .and(CIPipeline.Pipeline.projectId)
                            .is(ciPipelineFilterDTO.getProjectIdentifier());

    if (isNotBlank(ciPipelineFilterDTO.getPipelineName())) {
      criteria.and(CIPipeline.Pipeline.name).is(ciPipelineFilterDTO.getPipelineName());
    }
    if (isNotEmpty(ciPipelineFilterDTO.getTags())) {
      criteria.and(CIPipeline.Pipeline.tags).in(ciPipelineFilterDTO.getTags());
    }
    return criteria;
  }
}
