package io.harness.pms.ngpipeline.inputset.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.MergeHelper.getPipelineComponent;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTOPMS;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        .identifier(getStringField(yaml, "identifier", "inputSet"))
        .name(getStringField(yaml, "name", "inputSet"))
        .description(getStringField(yaml, "description", "inputSet"))
        .tags(TagMapper.convertToList(getTags(yaml, "inputSet")))
        .inputSetEntityType(InputSetEntityType.INPUT_SET)
        .yaml(yaml)
        .build();
  }
  public InputSetEntity toInputSetEntity(String accountId, String yaml) {
    String topKey = getTopKey(yaml);
    String orgIdentifier = getStringField(yaml, "orgIdentifier", topKey);
    String projectIdentifier = getStringField(yaml, "projectIdentifier", topKey);
    if (topKey.equals("inputSet")) {
      String pipelineComponent = getPipelineComponent(yaml);
      String pipelineIdentifier = PMSInputSetElementMapper.getStringField(pipelineComponent, "identifier", "pipeline");
      return toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    } else {
      String pipelineIdentifier = PMSInputSetElementMapper.getStringField(yaml, "pipelineIdentifier", topKey);
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
        .identifier(getStringField(yaml, "identifier", "overlayInputSet"))
        .name(getStringField(yaml, "name", "overlayInputSet"))
        .description(getStringField(yaml, "description", "overlayInputSet"))
        .tags(TagMapper.convertToList(getTags(yaml, "overlayInputSet")))
        .inputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET)
        .inputSetReferences(getReferences(yaml))
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
        .identifier(getStringField(yaml, "identifier", "inputSet"))
        .inputSetYaml(yaml)
        .name(getStringField(yaml, "name", "inputSet"))
        .description(getStringField(yaml, "description", "inputSet"))
        .tags(getTags(yaml, "inputSet"))
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
        .build();
  }

  public InputSetSummaryResponseDTOPMS toInputSetSummaryResponseDTOPMS(InputSetEntity entity) {
    return InputSetSummaryResponseDTOPMS.builder()
        .identifier(entity.getIdentifier())
        .name(entity.getName())
        .description(entity.getDescription())
        .pipelineIdentifier(entity.getPipelineIdentifier())
        .inputSetType(entity.getInputSetEntityType())
        .tags(TagMapper.convertToMap(entity.getTags()))
        .version(entity.getVersion())
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(entity))
        .build();
  }

  public String getStringField(String yaml, String fieldName, String topKey) {
    try {
      JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
      JsonNode innerMap = node.get(topKey);
      JsonNode field = innerMap.get(fieldName);
      if (field == null) {
        return null;
      }
      return innerMap.get(fieldName).asText().equals("") ? null : innerMap.get(fieldName).asText();
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert yaml to JsonNode");
    }
  }

  public boolean isPipelineAbsent(String yaml) {
    try {
      JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
      JsonNode innerMap = node.get("inputSet");
      JsonNode field = innerMap.get("pipeline");
      return field == null || field.toString().equals("{}");
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert yaml to JsonNode");
    }
  }

  private Map<String, String> getTags(String yaml, String topKey) {
    try {
      JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
      JsonNode innerMap = node.get(topKey);
      ObjectNode tags = (ObjectNode) innerMap.get("tags");
      if (tags == null) {
        return null;
      }
      Map<String, String> res = new LinkedHashMap<>();

      Set<String> fieldNames = new LinkedHashSet<>();
      tags.fieldNames().forEachRemaining(fieldNames::add);
      for (String key : fieldNames) {
        String value = tags.get(key).asText();
        res.put(key, value);
      }
      return res;
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert yaml to JsonNode");
    }
  }

  private String getTopKey(String yaml) {
    try {
      JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
      JsonNode innerMap = node.get("inputSet");
      if (innerMap == null) {
        return "overlayInputSet";
      }
      return "inputSet";
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert yaml to JsonNode");
    }
  }

  private List<String> getReferences(String yaml) {
    try {
      JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
      JsonNode innerMap = node.get("overlayInputSet");
      ArrayNode list = (ArrayNode) innerMap.get("inputSetReferences");
      List<String> res = new ArrayList<>();
      list.forEach(element -> res.add(element.asText()));
      return res;
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert yaml to JsonNode");
    }
  }
}
