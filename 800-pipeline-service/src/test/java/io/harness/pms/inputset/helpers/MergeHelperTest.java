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
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.InputSetErrorResponseDTOPMS;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.fqn.FQN;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MergeHelperTest extends CategoryTest {
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
  public void testCreateTemplateFromPipeline() throws IOException {
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
  public void testMergeInputSetIntoPipeline() throws IOException {
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
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInvalidFQNsInInputSet() throws IOException {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);
    String templateYaml = createTemplateFromPipeline(yaml);

    String inputSet = "runtimeInput1.yml";
    String inputSetYaml = readFile(inputSet);

    Set<FQN> invalidFQNs = getInvalidFQNsInInputSet(templateYaml, inputSetYaml).keySet();
    assertThat(invalidFQNs).isEmpty();

    String inputSetWrong = "runtimeInputWrong1.yml";
    String inputSetYamlWrong = readFile(inputSetWrong);

    invalidFQNs = getInvalidFQNsInInputSet(templateYaml, inputSetYamlWrong).keySet();
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
  public void testSanitizeInputSets() throws IOException {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);

    String wrongRuntimeInputFile = "runtimeInputWrong1.yml";
    String wrongRuntimeInput = readFile(wrongRuntimeInputFile);

    String sanitizedYaml1 = sanitizeRuntimeInput(yaml, wrongRuntimeInput);

    String inputSetWrongFile = "inputSetWrong1.yml";
    String inputSetWrongYaml = readFile(inputSetWrongFile);

    String sanitizedYaml2 = sanitizeInputSet(yaml, inputSetWrongYaml);

    String correctFile = "runtimeInput1.yml";
    String correctYaml = readFile(correctFile).replace("\"", "");
    assertThat(sanitizedYaml1.replace("\"", "")).isEqualTo(correctYaml);
    assertThat(sanitizedYaml2.replace("\"", "")).isEqualTo(correctYaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetErrorMap() throws IOException {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);

    String inputSetWrongFile = "inputSetWrong1.yml";
    String inputSetWrongYaml = readFile(inputSetWrongFile);

    InputSetErrorWrapperDTOPMS errorWrapperDTOPMS = MergeUtils.getErrorMap(yaml, inputSetWrongYaml);
    assertThat(errorWrapperDTOPMS).isNotNull();
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
    String inputSetYaml = readFile(inputSetFile);
    InputSetErrorWrapperDTOPMS emptyErrorWrapperDTOPMS = MergeUtils.getErrorMap(yaml, inputSetYaml);
    assertThat(emptyErrorWrapperDTOPMS).isNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeOnYamlWithFailureStrategies() throws IOException {
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
  public void testMergeOnCIPipelineYaml() throws IOException {
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
  public void testMergeOnPipelineWithEmptyListAndObject() throws IOException {
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
  public void testGetErrorMapForInputSetValidators() throws IOException {
    String yamlFile = "pipeline-with-input-set-validators.yaml";
    String pipelineYaml = readFile(yamlFile);

    String inputSetCorrectFile = "input-set-for-validators.yaml";
    String inputSetCorrect = readFile(inputSetCorrectFile);

    InputSetErrorWrapperDTOPMS errorMap = MergeUtils.getErrorMap(pipelineYaml, inputSetCorrect);
    assertThat(errorMap).isNull();

    String inputSetWrongFile = "wrong-input-set-for-validators.yaml";
    String inputSetWrong = readFile(inputSetWrongFile);

    InputSetErrorWrapperDTOPMS errorMapWrong = MergeUtils.getErrorMap(pipelineYaml, inputSetWrong);
    assertThat(errorMapWrong).isNotNull();
    Map<String, InputSetErrorResponseDTOPMS> uuidToErrorResponseMap = errorMapWrong.getUuidToErrorResponseMap();
    assertThat(uuidToErrorResponseMap.size()).isEqualTo(5);
    assertThat(uuidToErrorResponseMap.containsKey("pipeline.variables.[name:port2].value.")).isTrue();
    assertThat(
        uuidToErrorResponseMap.containsKey(
            "pipeline.stages.stage[identifier:qaStage].spec.service.serviceDefinition.spec.manifests.manifest[identifier:baseValues].spec.store.spec.connectorRef."))
        .isTrue();
    assertThat(uuidToErrorResponseMap.containsKey(
                   "pipeline.stages.stage[identifier:qaStage].spec.infrastructure.environment.tags.team."))
        .isTrue();
    assertThat(
        uuidToErrorResponseMap.containsKey(
            "pipeline.stages.stage[identifier:qaStage].spec.infrastructure.infrastructureDefinition.spec.namespace."))
        .isTrue();
    assertThat(
        uuidToErrorResponseMap.containsKey(
            "pipeline.stages.stage[identifier:qaStage].spec.execution.steps.step[identifier:httpStep2].spec.method."))
        .isTrue();
  }
}