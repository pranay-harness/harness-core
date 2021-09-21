package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidYamlException;
import io.harness.pms.stages.StageExecutionResponse.StageExecutionResponseBuilder;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class StageExecutionSelectorHelper {
  public List<StageExecutionResponse> getStageExecutionResponse(String pipelineYaml) {
    List<StageInfo> stagesInfo = getStageInfoList(pipelineYaml);

    List<StageExecutionResponse> executionResponses = new ArrayList<>();
    for (StageInfo stageInfo : stagesInfo) {
      StageExecutionResponseBuilder builder = StageExecutionResponse.builder()
                                                  .stageIdentifier(stageInfo.getIdentifier())
                                                  .stageName(stageInfo.getName())
                                                  .stagesRequired(stageInfo.getStagesRequired());
      if (stageInfo.getType().equals("Approval")) {
        builder.message("Running an approval stage individually can be redundant");
      }
      executionResponses.add(builder.build());
    }
    return executionResponses;
  }

  @VisibleForTesting
  List<StageInfo> getStageInfoList(String pipelineYaml) {
    List<StageInfo> stageYamlList = getStageYamlList(pipelineYaml);
    return addStagesRequired(stageYamlList);
  }

  @VisibleForTesting
  List<StageInfo> getStageYamlList(String pipelineYaml) {
    List<StageInfo> stageInfoList = new ArrayList<>();
    try {
      YamlField pipelineYamlField = YamlUtils.readTree(pipelineYaml);
      List<YamlNode> stagesYamlNodes = pipelineYamlField.getNode()
                                           .getField(YAMLFieldNameConstants.PIPELINE)
                                           .getNode()
                                           .getField(YAMLFieldNameConstants.STAGES)
                                           .getNode()
                                           .asArray();
      for (YamlNode stageYamlNode : stagesYamlNodes) {
        if (stageYamlNode.getField(YAMLFieldNameConstants.STAGE) != null) {
          stageInfoList.add(getBasicStageInfo(stageYamlNode));
          continue;
        }
        List<YamlNode> parallelStagesYamlNode =
            stageYamlNode.getField(YAMLFieldNameConstants.PARALLEL).getNode().asArray();
        for (YamlNode parallelStageYamlNode : parallelStagesYamlNode) {
          stageInfoList.add(getBasicStageInfo(parallelStageYamlNode));
        }
      }
      return stageInfoList;
    } catch (IOException e) {
      log.error("Could not read pipeline yaml while extracting stage yaml list. Yaml:\n" + pipelineYaml, e);
      throw new InvalidYamlException("Could not read pipeline yaml while extracting stage yaml list");
    }
  }

  private StageInfo getBasicStageInfo(YamlNode stageYamlNode) {
    String identifier = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getIdentifier();
    String name = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getName();
    String type = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getType();
    return StageInfo.builder().identifier(identifier).name(name).type(type).build();
  }

  @VisibleForTesting
  List<StageInfo> addStagesRequired(List<StageInfo> stageYamlList) {
    List<StageInfo> fullStageInfoList = new ArrayList<>();

    List<String> currRequiredStages = new ArrayList<>();
    boolean isPreviousApproval = false;
    for (StageInfo currStageInfo : stageYamlList) {
      boolean isCurrentApproval = currStageInfo.getType().equals("Approval");
      if (!isCurrentApproval) {
        fullStageInfoList.add(StageInfo.builder()
                                  .identifier(currStageInfo.getIdentifier())
                                  .name(currStageInfo.getName())
                                  .type(currStageInfo.getType())
                                  .stagesRequired(new ArrayList<>(currRequiredStages))
                                  .build());
        isPreviousApproval = false;
      } else if (!isPreviousApproval) {
        fullStageInfoList.add(StageInfo.builder()
                                  .identifier(currStageInfo.getIdentifier())
                                  .name(currStageInfo.getName())
                                  .type(currStageInfo.getType())
                                  .stagesRequired(Collections.emptyList())
                                  .build());
        currRequiredStages = new ArrayList<>();
        currRequiredStages.add(currStageInfo.getIdentifier());
        isPreviousApproval = true;
      } else {
        fullStageInfoList.add(StageInfo.builder()
                                  .identifier(currStageInfo.getIdentifier())
                                  .name(currStageInfo.getName())
                                  .type(currStageInfo.getType())
                                  .stagesRequired(new ArrayList<>(currRequiredStages))
                                  .build());
        currRequiredStages.add(currStageInfo.getIdentifier());
        isPreviousApproval = true;
      }
    }

    return fullStageInfoList;
  }

  @VisibleForTesting
  @Value
  @Builder
  class StageInfo {
    String identifier;
    String name;
    String type;
    List<String> stagesRequired;
  }
}
