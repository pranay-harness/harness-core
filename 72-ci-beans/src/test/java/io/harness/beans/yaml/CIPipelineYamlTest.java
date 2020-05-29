package io.harness.beans.yaml;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.CIBeansTest;
import io.harness.beans.CIPipeline;
import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;

public class CIPipelineYamlTest extends CIBeansTest {
  @Inject private YamlPipelineUtils yamlPipelineUtils;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testCiPipelineConversion() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("ci.yml");

    CIPipeline ciPipeline = yamlPipelineUtils.read(testFile, CIPipeline.class);
    assertThat(ciPipeline.getStages()).hasSize(1);
    assertThat(ciPipeline.getStages().get(0)).isInstanceOf(IntegrationStage.class);
  }

  // TODO: add more UTs
}