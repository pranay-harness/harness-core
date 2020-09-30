package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.redesign.services.CustomExecutionProvider;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

public class ManifestYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testParseManifestsYaml() throws Exception {
    String file = CustomExecutionProvider.class.getClassLoader().getResource("cdng/pipeline.yaml").getFile();
    String fileContent = FileUtils.readFileToString(new File(file), "UTF-8");

    NgPipeline pipeline = YamlPipelineUtils.read(fileContent, NgPipeline.class);
    assertThat(pipeline).isNotNull();
  }
}
