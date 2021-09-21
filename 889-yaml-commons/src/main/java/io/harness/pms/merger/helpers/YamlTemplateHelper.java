package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class YamlTemplateHelper {
  public String createTemplateFromPipeline(String pipelineYaml) {
    return createTemplate(pipelineYaml, true, null);
  }

  public String createTemplateFromPipelineForGivenStages(String pipelineYaml, List<String> stageIdentifiers) {
    return createTemplate(pipelineYaml, true, stageIdentifiers);
  }

  public String removeRuntimeInputFromYaml(String runtimeInputYaml) {
    return createTemplate(runtimeInputYaml, false, null);
  }

  public String createTemplateFromYaml(String templateYaml) {
    return createTemplate(templateYaml, true, null);
  }

  private String createTemplate(String yaml, boolean keepInput, List<String> stageIdentifiers) {
    YamlConfig yamlConfig = new YamlConfig(yaml);
    Map<FQN, Object> fullMap = yamlConfig.getFqnToValueMap();
    Map<FQN, Object> templateMap = new LinkedHashMap<>();
    fullMap.keySet().forEach(key -> {
      String value = fullMap.get(key).toString().replace("\"", "");
      if ((keepInput && NGExpressionUtils.matchesInputSetPattern(value))
          || (!keepInput && !NGExpressionUtils.matchesInputSetPattern(value) && !key.isIdentifierOrVariableName()
              && !key.isType())) {
        templateMap.put(key, fullMap.get(key));
      }
    });
    if (EmptyPredicate.isNotEmpty(stageIdentifiers)) {
      FQNHelper.removeNonRequiredStages(templateMap, stageIdentifiers);
    }
    return (new YamlConfig(templateMap, yamlConfig.getYamlMap())).getYaml();
  }
}
