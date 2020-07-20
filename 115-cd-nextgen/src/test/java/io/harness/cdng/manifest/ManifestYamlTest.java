package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.redesign.services.CustomExecutionUtils;
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
    String file = CustomExecutionUtils.class.getClassLoader().getResource("cdng/pipeline.yaml").getFile();
    String fileContent = FileUtils.readFileToString(new File(file), "UTF-8");

    CDPipeline pipeline = YamlPipelineUtils.read(fileContent, CDPipeline.class);
    assertThat(pipeline).isNotNull();
  }
}
