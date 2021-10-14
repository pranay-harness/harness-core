package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.template.TemplateApplyRequestDTO;
import io.harness.ng.core.template.TemplateInputsErrorResponseDTO;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.exception.NGTemplateResolveException;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.remote.client.NGRestUtils;
import io.harness.template.remote.TemplateResourceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineTemplateHelper {
  private final PmsFeatureFlagHelper pmsFeatureFlagHelper;
  private final TemplateResourceClient templateResourceClient;

  public String resolveTemplateRefsInPipeline(PipelineEntity pipelineEntity) {
    return resolveTemplateRefsInPipeline(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), pipelineEntity.getYaml());
  }

  public String resolveTemplateRefsInPipeline(String accountId, String orgId, String projectId, String yaml) {
    if (pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.NG_TEMPLATES)) {
      try {
        TemplateMergeResponseDTO templateMergeResponseDTO =
            NGRestUtils.getResponse(templateResourceClient.applyTemplatesOnGivenYaml(
                accountId, orgId, projectId, TemplateApplyRequestDTO.builder().originalEntityYaml(yaml).build()));
        if (!templateMergeResponseDTO.isValid()) {
          throw new NGTemplateResolveException("Exception in resolving template refs in given pipeline yaml.", USER,
              templateMergeResponseDTO.getErrorResponse());
        }
        return templateMergeResponseDTO.getMergedPipelineYaml();
      } catch (Exception e) {
        throw new NGTemplateResolveException("Exception in resolving template refs in given pipeline yaml.", USER,
            new TemplateInputsErrorResponseDTO(Status.ERROR, ErrorCode.TEMPLATE_EXCEPTION,
                "Exception while resolving template refs.", "", "", new HashMap<>()));
      }
    }
    return yaml;
  }
}
