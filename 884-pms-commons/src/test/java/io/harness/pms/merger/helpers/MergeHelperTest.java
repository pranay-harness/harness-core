package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSetIntoPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSets;
import static io.harness.pms.merger.helpers.YamlTemplateHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.YamlTemplateHelper.createTemplateFromPipelineForGivenStages;
import static io.harness.rule.OwnerRule.GARVIT;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class MergeHelperTest extends CategoryTest {
  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  private void assertStringEqualToFile(String result, String filename) {
    String expected = readFile(filename);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateTemplateFromPipeline() {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);
    String templateYaml = createTemplateFromPipeline(yaml);

    String resFile = "pipeline-extensive-template.yml";
    String resTemplate = readFile(resFile);
    assertThat(templateYaml).isEqualTo(resTemplate);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateTemplateFromPipelineForGivenStages() {
    String pipeline1 = "pipeline-extensive.yml";
    String pipelineYaml1 = readFile(pipeline1);
    String templateForQaStage4 =
        createTemplateFromPipelineForGivenStages(pipelineYaml1, Collections.singletonList("qaStage4"));
    assertStringEqualToFile(templateForQaStage4, "templateForQaStage4.yaml");
    String templateForQaStage3 =
        createTemplateFromPipelineForGivenStages(pipelineYaml1, Collections.singletonList("qaStage3"));
    assertStringEqualToFile(templateForQaStage3, "templateForQaStage3.yaml");
    String templateForQaStageAndQaStage3 =
        createTemplateFromPipelineForGivenStages(pipelineYaml1, Arrays.asList("qaStage", "qaStage3"));
    assertStringEqualToFile(templateForQaStageAndQaStage3, "templateForQaStageAndQaStage3.yaml");

    String pipeline2 = "pipeline-2.yaml";
    String pipelineYaml2 = readFile(pipeline2);
    String template = createTemplateFromPipelineForGivenStages(pipelineYaml2, Collections.singletonList("qaStage4"));
    assertThat(template).isNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSetIntoPipeline() {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);

    String inputSet = "runtimeInput1.yml";
    String inputSetYaml = readFile(inputSet);

    String res = mergeInputSetIntoPipeline(yaml, inputSetYaml, false);
    String resYaml = res.replace("\"", "");

    String mergedYamlFile = "pipeline-extensive-merged.yml";
    String mergedYaml = readFile(mergedYamlFile);

    assertThat(resYaml).isEqualTo(mergedYaml);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testMergeInputSetIntoPipelineInputSetValidator() {
    String filename = "pipeline-inputset-validator.yml";
    String yaml = readFile(filename);

    String inputSet = "runtime-inputset-validator.yml";
    String inputSetYaml = readFile(inputSet);

    String res = mergeInputSetIntoPipeline(yaml, inputSetYaml, true);
    String resYaml = res.replace("\"", "");

    String mergedYamlFile = "pipeline-inputset-validator-merged.yml";
    String mergedYaml = readFile(mergedYamlFile);

    assertThat(resYaml).isEqualTo(mergedYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSetIntoServiceDependenciesPipeline() {
    String filename = "service-dependencies-pipeline.yaml";
    String yaml = readFile(filename);

    String inputSet = "service-dependencies-runtime-input.yaml";
    String inputSetYaml = readFile(inputSet);

    String res = mergeInputSetIntoPipeline(yaml, inputSetYaml, false);
    String resYaml = res.replace("\"", "");

    String mergedYamlFile = "service-dependencies-pipeline-merged.yaml";
    String mergedYaml = readFile(mergedYamlFile);

    assertThat(resYaml).isEqualTo(mergedYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSets() {
    String inputSet1 = "inputSet1.yml";
    String inputSetYaml1 = readFile(inputSet1);
    String inputSet2 = "inputSet2.yml";
    String inputSetYaml2 = readFile(inputSet2);
    List<String> inputSetYamlList = new ArrayList<>();
    inputSetYamlList.add(inputSetYaml1);
    inputSetYamlList.add(inputSetYaml2);

    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);
    String templateYaml = createTemplateFromPipeline(yaml);

    String mergedYaml = mergeInputSets(templateYaml, inputSetYamlList, false);

    String inputSetMerged = "input12-merged.yml";
    String inputSetYamlMerged = readFile(inputSetMerged);
    assertThat(mergedYaml).isEqualTo(inputSetYamlMerged);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnYamlWithFailureStrategies() {
    String fullYamlFile = "failure-strategy.yaml";
    String fullYaml = readFile(fullYamlFile);
    String templateOfFull = createTemplateFromPipeline(fullYaml);
    assertThat(templateOfFull).isNull();

    String yamlWithRuntimeFile = "failure-strategy-with-runtime-input.yaml";
    String yamlWithRuntime = readFile(yamlWithRuntimeFile);
    String template = createTemplateFromPipeline(yamlWithRuntime);

    String templateFile = "failure-strategy-template.yaml";
    String templateActual = readFile(templateFile);
    assertThat(template.replace("\"", "")).isEqualTo(templateActual);

    String runtimeInputFile = "failure-strategy-runtime-input.yaml";
    String runtimeInput = readFile(runtimeInputFile);
    String mergedYaml = mergeInputSetIntoPipeline(yamlWithRuntime, runtimeInput, false);
    assertThat(mergedYaml.replace("\"", "")).isEqualTo(fullYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnCIPipelineYaml() {
    String fullYamlFile = "ci-pipeline-with-reports.yaml";
    String fullYaml = readFile(fullYamlFile);
    String templateOfFull = createTemplateFromPipeline(fullYaml);
    assertThat(templateOfFull).isNull();

    String yamlWithRuntimeFile = "ci-pipeline-runtime-input.yaml";
    String yamlWithRuntime = readFile(yamlWithRuntimeFile);
    String template = createTemplateFromPipeline(yamlWithRuntime);

    String templateFile = "ci-pipeline-template.yaml";
    String templateActual = readFile(templateFile);
    assertThat(template.replace("\"", "")).isEqualTo(templateActual);

    String runtimeInputFile = "ci-runtime-input-yaml.yaml";
    String runtimeInput = readFile(runtimeInputFile);
    String mergedYaml = mergeInputSetIntoPipeline(yamlWithRuntime, runtimeInput, false);
    assertThat(mergedYaml.replace("\"", "")).isEqualTo(fullYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnPipelineWithEmptyListAndObject() {
    String yamlFile = "empty-object-and-list-with-runtime.yaml";
    String yaml = readFile(yamlFile);
    String template = createTemplateFromPipeline(yaml);
    assertThat(template).isNotNull();

    String runtimeInputFile = "empty-object-and-list-runtime.yaml";
    String runtimeInput = readFile(runtimeInputFile);
    String mergedYaml = mergeInputSetIntoPipeline(yaml, runtimeInput, false);

    String fullYamlFile = "empty-object-and-list.yaml";
    String fullYaml = readFile(fullYamlFile);
    assertThat(mergedYaml).isEqualTo(fullYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnPipelineWithHelmCommandFlags() {
    String yamlFile = "helm-command-flags-pipeline.yaml";
    String yaml = readFile(yamlFile);
    String template = createTemplateFromPipeline(yaml);
    assertThat(template).isNotNull();

    String runtimeInputFile = "helm-command-flags-runtime-input.yaml";
    String runtimeInput = readFile(runtimeInputFile);
    String mergedYaml = mergeInputSetIntoPipeline(yaml, runtimeInput, false);
    String fullYamlFile = "helm-command-flags-merged-pipeline.yaml";
    String fullYaml = readFile(fullYamlFile);
    assertThat(mergedYaml).isEqualTo(fullYaml);
  }
}
