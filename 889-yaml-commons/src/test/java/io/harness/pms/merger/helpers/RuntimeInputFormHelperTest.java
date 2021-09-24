package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.RuntimeInputFormHelper.createRuntimeInputForm;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class RuntimeInputFormHelperTest extends CategoryTest {
  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateTemplateFromPipeline() {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);
    String templateYaml = createRuntimeInputForm(yaml, true);

    String resFile = "pipeline-extensive-template.yml";
    String resTemplate = readFile(resFile);
    assertThat(templateYaml).isEqualTo(resTemplate);
  }
}