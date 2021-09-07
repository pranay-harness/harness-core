package io.harness.beans.yaml;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.rule.Owner;
import io.harness.utils.YamlPipelineUtils;
import io.harness.yaml.core.properties.CIProperties;
import io.harness.yaml.core.properties.NGProperties;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(CI)
public class CIPmsPipelineYamlTest extends CiBeansTestBase {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testCiPipelineConversion() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cipms.yml");
    PipelineConfig ngPipelineActual = YamlPipelineUtils.read(testFile, PipelineConfig.class);
    assertThat(ngPipelineActual).isNotNull();

    NGProperties properties = ngPipelineActual.getPipelineInfoConfig().getProperties();
    CIProperties ciProperties = YamlPipelineUtils.read(properties.getCi().toString(), CIProperties.class);
    log.info(ciProperties.toString());

    List<StageElementWrapperConfig> stages = ngPipelineActual.getPipelineInfoConfig().getStages();
    for (StageElementWrapperConfig stage : stages) {
      StageElementConfig stageElementConfig =
          YamlPipelineUtils.read(stage.getStage().toString(), StageElementConfig.class);

      assertThat(stageElementConfig).isNotNull();

      IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();
      if (integrationStageConfig.getExecution() != null) {
        for (ExecutionWrapperConfig executionWrapperConfig : integrationStageConfig.getExecution().getSteps()) {
          StepElementConfig stepElementConfig =
              YamlPipelineUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
          assertThat(stepElementConfig).isNotNull();
        }
      }
    }
  }
}
