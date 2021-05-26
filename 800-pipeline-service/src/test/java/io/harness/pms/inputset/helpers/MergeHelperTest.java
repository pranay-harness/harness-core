package io.harness.pms.inputset.helpers;

import static io.harness.pms.merger.helpers.MergeHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.getInvalidFQNsInInputSet;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSetIntoPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSets;
import static io.harness.pms.merger.helpers.MergeHelper.sanitizeInputSet;
import static io.harness.pms.merger.helpers.MergeHelper.sanitizeRuntimeInput;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.fqn.FQN;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MergeHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateTemplateFromPipeline() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    String templateYaml = createTemplateFromPipeline(yaml);

    String resFile = "pipeline-extensive-template.yml";
    String resTemplate =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(resFile)), StandardCharsets.UTF_8);
    assertThat(templateYaml).isEqualTo(resTemplate);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSetIntoPipeline() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    String inputSet = "runtimeInput1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);

    String res = mergeInputSetIntoPipeline(yaml, inputSetYaml, false);
    String resYaml = res.replace("\"", "");

    String mergedYamlFile = "pipeline-extensive-merged.yml";
    String mergedYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(mergedYamlFile)), StandardCharsets.UTF_8);

    assertThat(resYaml).isEqualTo(mergedYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInvalidFQNsInInputSet() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    String templateYaml = createTemplateFromPipeline(yaml);

    String inputSet = "runtimeInput1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);

    Set<FQN> invalidFQNs = getInvalidFQNsInInputSet(templateYaml, inputSetYaml);
    assertThat(invalidFQNs).isEmpty();

    String inputSetWrong = "runtimeInputWrong1.yml";
    String inputSetYamlWrong =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetWrong)), StandardCharsets.UTF_8);

    invalidFQNs = getInvalidFQNsInInputSet(templateYaml, inputSetYamlWrong);
    assertThat(invalidFQNs.size()).isEqualTo(2);
    String invalidFQN1 =
        "pipeline.stages.stage[identifier:qaStage].spec.execution.steps.step[identifier:httpStep1].spec.method.";
    String invalidFQN2 = "pipeline.stages.stage[identifier:qaStage].absolutelyWrongKey.";
    assertThat(invalidFQNs.stream().map(FQN::display).collect(Collectors.toList()).contains(invalidFQN1)).isTrue();
    assertThat(invalidFQNs.stream().map(FQN::display).collect(Collectors.toList()).contains(invalidFQN2)).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSets() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputSet1 = "inputSet1.yml";
    String inputSetYaml1 =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet1)), StandardCharsets.UTF_8);
    String inputSet2 = "inputSet2.yml";
    String inputSetYaml2 =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet2)), StandardCharsets.UTF_8);
    List<String> inputSetYamlList = new ArrayList<>();
    inputSetYamlList.add(inputSetYaml1);
    inputSetYamlList.add(inputSetYaml2);

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    String templateYaml = createTemplateFromPipeline(yaml);

    String mergedYaml = mergeInputSets(templateYaml, inputSetYamlList, false);

    String inputSetMerged = "input12-merged.yml";
    String inputSetYamlMerged =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetMerged)), StandardCharsets.UTF_8);
    assertThat(mergedYaml).isEqualTo(inputSetYamlMerged);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testSanitizeInputSets() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    String wrongRuntimeInputFile = "runtimeInputWrong1.yml";
    String wrongRuntimeInput = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(wrongRuntimeInputFile)), StandardCharsets.UTF_8);

    String sanitizedYaml1 = sanitizeRuntimeInput(yaml, wrongRuntimeInput);

    String inputSetWrongFile = "inputSetWrong1.yml";
    String inputSetWrongYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetWrongFile)), StandardCharsets.UTF_8);

    String sanitizedYaml2 = sanitizeInputSet(yaml, inputSetWrongYaml);

    String correctFile = "runtimeInput1.yml";
    String correctYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(correctFile)), StandardCharsets.UTF_8)
            .replace("\"", "");
    assertThat(sanitizedYaml1.replace("\"", "")).isEqualTo(correctYaml);
    assertThat(sanitizedYaml2.replace("\"", "")).isEqualTo(correctYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetErrorMap() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    String inputSetWrongFile = "inputSetWrong1.yml";
    String inputSetWrongYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetWrongFile)), StandardCharsets.UTF_8);

    InputSetErrorWrapperDTOPMS errorWrapperDTOPMS = MergeHelper.getErrorMap(yaml, inputSetWrongYaml);
    assertThat(errorWrapperDTOPMS.getErrorPipelineYaml())
        .isEqualTo("pipeline:\n"
            + "  identifier: \"Test_Pipline11\"\n"
            + "  stages:\n"
            + "  - stage:\n"
            + "      identifier: \"qaStage\"\n"
            + "      type: \"Deployment\"\n"
            + "      spec:\n"
            + "        execution:\n"
            + "          steps:\n"
            + "          - step:\n"
            + "              identifier: \"httpStep1\"\n"
            + "              type: \"Http\"\n"
            + "              spec:\n"
            + "                method: \"pipeline.stages.stage[identifier:qaStage].spec.execution.steps.step[identifier:httpStep1].spec.method.\"\n");
    assertThat(errorWrapperDTOPMS.getUuidToErrorResponseMap().size()).isEqualTo(1);
    assertThat(
        errorWrapperDTOPMS.getUuidToErrorResponseMap().containsKey(
            "pipeline.stages.stage[identifier:qaStage].spec.execution.steps.step[identifier:httpStep1].spec.method."))
        .isTrue();

    String inputSetFile = "inputSet1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFile)), StandardCharsets.UTF_8);
    InputSetErrorWrapperDTOPMS emptyErrorWrapperDTOPMS = MergeHelper.getErrorMap(yaml, inputSetYaml);
    assertThat(emptyErrorWrapperDTOPMS).isNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnYamlWithFailureStrategies() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String fullYamlFile = "failure-strategy.yaml";
    String fullYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(fullYamlFile)), StandardCharsets.UTF_8);
    String templateOfFull = createTemplateFromPipeline(fullYaml);
    assertThat(templateOfFull).isNull();

    String yamlWithRuntimeFile = "failure-strategy-with-runtime-input.yaml";
    String yamlWithRuntime = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(yamlWithRuntimeFile)), StandardCharsets.UTF_8);
    String template = createTemplateFromPipeline(yamlWithRuntime);

    String templateFile = "failure-strategy-template.yaml";
    String templateActual =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(templateFile)), StandardCharsets.UTF_8);
    assertThat(template.replace("\"", "")).isEqualTo(templateActual);

    String runtimeInputFile = "failure-strategy-runtime-input.yaml";
    String runtimeInput =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(runtimeInputFile)), StandardCharsets.UTF_8);
    String mergedYaml = mergeInputSetIntoPipeline(yamlWithRuntime, runtimeInput, false);
    assertThat(mergedYaml.replace("\"", "")).isEqualTo(fullYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnCIPipelineYaml() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String fullYamlFile = "ci-pipeline-with-reports.yaml";
    String fullYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(fullYamlFile)), StandardCharsets.UTF_8);
    String templateOfFull = createTemplateFromPipeline(fullYaml);
    assertThat(templateOfFull).isNull();

    String yamlWithRuntimeFile = "ci-pipeline-runtime-input.yaml";
    String yamlWithRuntime = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(yamlWithRuntimeFile)), StandardCharsets.UTF_8);
    String template = createTemplateFromPipeline(yamlWithRuntime);

    String templateFile = "ci-pipeline-template.yaml";
    String templateActual =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(templateFile)), StandardCharsets.UTF_8);
    assertThat(template.replace("\"", "")).isEqualTo(templateActual);

    String runtimeInputFile = "ci-runtime-input-yaml.yaml";
    String runtimeInput =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(runtimeInputFile)), StandardCharsets.UTF_8);
    String mergedYaml = mergeInputSetIntoPipeline(yamlWithRuntime, runtimeInput, false);
    assertThat(mergedYaml.replace("\"", "")).isEqualTo(fullYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnPipelineWithEmptyListAndObject() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "empty-object-and-list-with-runtime.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);
    String template = createTemplateFromPipeline(yaml);
    assertThat(template).isNotNull();

    String runtimeInputFile = "empty-object-and-list-runtime.yaml";
    String runtimeInput =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(runtimeInputFile)), StandardCharsets.UTF_8);
    String mergedYaml = mergeInputSetIntoPipeline(yaml, runtimeInput, false);

    String fullYamlFile = "empty-object-and-list.yaml";
    String fullYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(fullYamlFile)), StandardCharsets.UTF_8);
    assertThat(mergedYaml).isEqualTo(fullYaml);
  }
}