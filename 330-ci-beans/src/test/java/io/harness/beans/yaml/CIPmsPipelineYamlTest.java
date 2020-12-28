package io.harness.beans.yaml;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.CiBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CIPmsPipelineYamlTest extends CiBeansTestBase {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testCiPipelineConversion() throws IOException {
    //    ClassLoader classLoader = this.getClass().getClassLoader();
    //    final URL testFile = classLoader.getResource("cipms.yml");
    //    PipelineConfig ngPipelineActual = YamlPipelineUtils.read(testFile, PipelineConfig.class);
    //    assertThat(ngPipelineActual).isNotNull();
    //
    //    NGProperties properties = ngPipelineActual.getPipelineInfoConfig().getProperties();
    //    CIProperties ciProperties = YamlPipelineUtils.read(properties.getCi().toString(), CIProperties.class);
    //    log.info(ciProperties.toString());
    //
    //    List<StageElementWrapperConfig> stages = ngPipelineActual.getPipelineInfoConfig().getStages();
    //    for (StageElementWrapperConfig stage : stages) {
    //      StageElementConfig stageElementConfig =
    //          YamlPipelineUtils.read(stage.getStage().toString(), StageElementConfig.class);
    //
    //      assertThat(stageElementConfig).isNotNull();
    //
    //      IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();
    //      if (integrationStageConfig.getExecution() != null) {
    //        for (ExecutionWrapperConfig executionWrapperConfig : integrationStageConfig.getExecution().getSteps()) {
    //          StepElementConfig stepElementConfig =
    //              YamlPipelineUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    //          assertThat(stepElementConfig).isNotNull();
    //        }
    //      }
    //    }
  }
}
