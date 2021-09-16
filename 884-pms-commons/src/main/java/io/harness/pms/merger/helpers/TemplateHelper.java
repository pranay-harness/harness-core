package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class TemplateHelper {
  public String createTemplateFromPipeline(String pipelineYaml) {
    return createTemplateFromPipeline(pipelineYaml, true);
  }

  public String removeRuntimeInputFromYaml(String runtimeInputYaml) {
    return createTemplateFromPipeline(runtimeInputYaml, false);
  }

  private String createTemplateFromPipeline(String pipelineYaml, boolean keepInput) {
    PipelineYamlConfig pipeline = new PipelineYamlConfig(pipelineYaml);
    Map<FQN, Object> fullMap = pipeline.getFqnToValueMap();
    Map<FQN, Object> templateMap = new LinkedHashMap<>();
    fullMap.keySet().forEach(key -> {
      String value = fullMap.get(key).toString().replace("\"", "");
      if ((keepInput && NGExpressionUtils.matchesInputSetPattern(value))
          || (!keepInput && !NGExpressionUtils.matchesInputSetPattern(value) && !key.isIdentifierOrVariableName()
              && !key.isType())) {
        templateMap.put(key, fullMap.get(key));
      }
    });
    return (new PipelineYamlConfig(templateMap, pipeline.getYamlMap())).getYaml();
  }
}
