/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.InputSetYamlHelper.getPipelineComponent;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetReference;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.gitsync.sdk.EntityValidityDetails;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.helpers.InputSetYamlHelper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTOPMS;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;

import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PMSInputSetElementMapper {
  public InputSetEntity toInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    return InputSetEntity.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .pipelineIdentifier(pipelineIdentifier)
        .identifier(InputSetYamlHelper.getStringField(yaml, "identifier", "inputSet"))
        .name(InputSetYamlHelper.getStringField(yaml, "name", "inputSet"))
        .description(InputSetYamlHelper.getStringField(yaml, "description", "inputSet"))
        .tags(TagMapper.convertToList(InputSetYamlHelper.getTags(yaml, "inputSet")))
        .inputSetEntityType(InputSetEntityType.INPUT_SET)
        .yaml(yaml)
        .build();
  }
  public InputSetEntity toInputSetEntity(String accountId, String yaml) {
    String topKey = InputSetYamlHelper.getRootNodeOfInputSetYaml(yaml);
    String orgIdentifier = InputSetYamlHelper.getStringField(yaml, "orgIdentifier", topKey);
    String projectIdentifier = InputSetYamlHelper.getStringField(yaml, "projectIdentifier", topKey);
    if (topKey.equals("inputSet")) {
      String pipelineComponent = getPipelineComponent(yaml);
      String pipelineIdentifier = InputSetYamlHelper.getStringField(pipelineComponent, "identifier", "pipeline");
      return toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    } else {
      String pipelineIdentifier = InputSetYamlHelper.getStringField(yaml, "pipelineIdentifier", topKey);
      return toInputSetEntityForOverlay(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    }
  }

  public InputSetEntity toInputSetEntityForOverlay(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    return InputSetEntity.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .pipelineIdentifier(pipelineIdentifier)
        .identifier(InputSetYamlHelper.getStringField(yaml, "identifier", "overlayInputSet"))
        .name(InputSetYamlHelper.getStringField(yaml, "name", "overlayInputSet"))
        .description(InputSetYamlHelper.getStringField(yaml, "description", "overlayInputSet"))
        .tags(TagMapper.convertToList(InputSetYamlHelper.getTags(yaml, "overlayInputSet")))
        .inputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET)
        .inputSetReferences(InputSetYamlHelper.getReferencesFromOverlayInputSetYaml(yaml))
        .yaml(yaml)
        .build();
  }

  public InputSetResponseDTOPMS toInputSetResponseDTOPMS(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String yaml, InputSetErrorWrapperDTOPMS errorWrapperDTO) {
    return InputSetResponseDTOPMS.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .pipelineIdentifier(pipelineIdentifier)
        .identifier(InputSetYamlHelper.getStringField(yaml, "identifier", "inputSet"))
        .inputSetYaml(yaml)
        .name(InputSetYamlHelper.getStringField(yaml, "name", "inputSet"))
        .description(InputSetYamlHelper.getStringField(yaml, "description", "inputSet"))
        .tags(InputSetYamlHelper.getTags(yaml, "inputSet"))
        .isErrorResponse(true)
        .inputSetErrorWrapper(errorWrapperDTO)
        .build();
  }

  public InputSetResponseDTOPMS toInputSetResponseDTOPMS(InputSetEntity entity) {
    return InputSetResponseDTOPMS.builder()
        .accountId(entity.getAccountId())
        .orgIdentifier(entity.getOrgIdentifier())
        .projectIdentifier(entity.getProjectIdentifier())
        .pipelineIdentifier(entity.getPipelineIdentifier())
        .identifier(entity.getIdentifier())
        .inputSetYaml(entity.getYaml())
        .name(entity.getName())
        .description(entity.getDescription())
        .tags(TagMapper.convertToMap(entity.getTags()))
        .version(entity.getVersion())
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(entity))
        .isOutdated(entity.getIsInvalid())
        .entityValidityDetails(entity.isEntityInvalid()
                ? EntityValidityDetails.builder().valid(false).invalidYaml(entity.getYaml()).build()
                : EntityValidityDetails.builder().valid(true).build())
        .build();
  }

  public OverlayInputSetResponseDTOPMS toOverlayInputSetResponseDTOPMS(InputSetEntity entity) {
    return toOverlayInputSetResponseDTOPMS(entity, false, null);
  }

  public OverlayInputSetResponseDTOPMS toOverlayInputSetResponseDTOPMS(
      InputSetEntity entity, boolean isError, Map<String, String> invalidReferences) {
    return OverlayInputSetResponseDTOPMS.builder()
        .accountId(entity.getAccountId())
        .orgIdentifier(entity.getOrgIdentifier())
        .projectIdentifier(entity.getProjectIdentifier())
        .pipelineIdentifier(entity.getPipelineIdentifier())
        .overlayInputSetYaml(entity.getYaml())
        .identifier(entity.getIdentifier())
        .name(entity.getName())
        .description(entity.getDescription())
        .tags(TagMapper.convertToMap(entity.getTags()))
        .inputSetReferences(entity.getInputSetReferences())
        .version(entity.getVersion())
        .isErrorResponse(isError)
        .invalidInputSetReferences(invalidReferences)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(entity))
        .isOutdated(entity.getIsInvalid())
        .entityValidityDetails(entity.isEntityInvalid()
                ? EntityValidityDetails.builder().valid(false).invalidYaml(entity.getYaml()).build()
                : EntityValidityDetails.builder().valid(true).build())
        .build();
  }

  public InputSetSummaryResponseDTOPMS toInputSetSummaryResponseDTOPMS(InputSetEntity entity,
      InputSetErrorWrapperDTOPMS inputSetErrorDetails, Map<String, String> overlaySetErrorDetails) {
    return InputSetSummaryResponseDTOPMS.builder()
        .identifier(entity.getIdentifier())
        .name(entity.getName())
        .description(entity.getDescription())
        .pipelineIdentifier(entity.getPipelineIdentifier())
        .inputSetType(entity.getInputSetEntityType())
        .tags(TagMapper.convertToMap(entity.getTags()))
        .version(entity.getVersion())
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(entity))
        .createdAt(entity.getCreatedAt())
        .lastUpdatedAt(entity.getLastUpdatedAt())
        .isOutdated(entity.getIsInvalid())
        .inputSetErrorDetails(inputSetErrorDetails)
        .overlaySetErrorDetails(overlaySetErrorDetails)
        .entityValidityDetails(entity.isEntityInvalid()
                ? EntityValidityDetails.builder().valid(false).invalidYaml(entity.getYaml()).build()
                : EntityValidityDetails.builder().valid(true).build())
        .build();
  }

  public EntityDetail toEntityDetail(InputSetEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.INPUT_SETS)
        .entityRef(InputSetReference.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .pipelineIdentifier(entity.getPipelineIdentifier())
                       .identifier(entity.getIdentifier())
                       .build())
        .build();
  }
}
