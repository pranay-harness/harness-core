package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidYamlException;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.utils.YamlPipelineUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class StagesExpressionExtractor {
  List<BasicStageInfo> getStageYamlList(String pipelineYaml, List<String> stageIdentifiers) {
    try {
      List<BasicStageInfo> stageYamlList = new ArrayList<>();
      YamlField pipelineYamlField = YamlUtils.readTree(pipelineYaml);
      List<YamlNode> stagesYamlNodes = pipelineYamlField.getNode()
                                           .getField(YAMLFieldNameConstants.PIPELINE)
                                           .getNode()
                                           .getField(YAMLFieldNameConstants.STAGES)
                                           .getNode()
                                           .asArray();
      for (YamlNode stageYamlNode : stagesYamlNodes) {
        if (stageYamlNode.getField(YAMLFieldNameConstants.STAGE) != null) {
          BasicStageInfo basicStageInfoWithYaml = getBasicStageInfoWithYaml(stageYamlNode);
          if (stageIdentifiers.contains(basicStageInfoWithYaml.getIdentifier())) {
            stageYamlList.add(basicStageInfoWithYaml);
          }
          continue;
        }
        List<YamlNode> parallelStagesYamlNode =
            stageYamlNode.getField(YAMLFieldNameConstants.PARALLEL).getNode().asArray();
        for (YamlNode parallelStageYamlNode : parallelStagesYamlNode) {
          BasicStageInfo basicStageInfoWithYaml = getBasicStageInfoWithYaml(parallelStageYamlNode);
          if (stageIdentifiers.contains(basicStageInfoWithYaml.getIdentifier())) {
            stageYamlList.add(basicStageInfoWithYaml);
          }
        }
      }
      return stageYamlList;
    } catch (IOException e) {
      log.error("Could not read pipeline yaml while extracting stage yaml list. Yaml:\n" + pipelineYaml, e);
      throw new InvalidYamlException("Could not read pipeline yaml while extracting stage yaml list");
    }
  }

  private BasicStageInfo getBasicStageInfoWithYaml(YamlNode stageYamlNode) throws IOException {
    String identifier = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getIdentifier();
    String name = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getName();
    String type = stageYamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getType();
    String yaml = YamlPipelineUtils.getYamlString(stageYamlNode.getCurrJsonNode());
    return BasicStageInfo.builder().identifier(identifier).name(name).type(type).yaml(yaml).build();
  }

  List<String> getListOfExpressions(String stageYaml) {
    return NGExpressionUtils.getListOfExpressions(stageYaml);
  }
}
