package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;

public class ManifestYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseManifestsYaml() throws Exception {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/pipelineWithRuntimeInput.yml");
    NgPipeline ngPipeline = YamlPipelineUtils.read(testFile, NgPipeline.class);

    assertThat(ngPipeline).isNotNull();
  }
}
