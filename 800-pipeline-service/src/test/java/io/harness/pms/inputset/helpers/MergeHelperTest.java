package io.harness.pms.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.inputset.fqn.FQN;
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

public class MergeHelperTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateTemplateFromPipeline() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    String templateYaml = MergeHelper.createTemplateFromPipeline(yaml, true);

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

    String inputSet = "inputSet1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);

    String res = MergeHelper.mergeInputSetIntoPipeline(yaml, inputSetYaml, true);
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
    String templateYaml = MergeHelper.createTemplateFromPipeline(yaml, true);

    String inputSet = "inputSet1.yml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet)), StandardCharsets.UTF_8);

    Set<FQN> invalidFQNs = MergeHelper.getInvalidFQNsInInputSet(templateYaml, inputSetYaml);
    assertThat(invalidFQNs).isEmpty();

    String inputSetWrong = "inputSet1Wrong.yml";
    String inputSetYamlWrong =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetWrong)), StandardCharsets.UTF_8);

    invalidFQNs = MergeHelper.getInvalidFQNsInInputSet(templateYaml, inputSetYamlWrong);
    assertThat(invalidFQNs.size()).isEqualTo(1);
    String invalidFQN =
        "pipeline.stages.stage[identifier:qaStage].spec.execution.steps.step[identifier:httpStep1].spec.method.";
    assertThat(invalidFQNs.stream().map(FQN::display).collect(Collectors.toList()).contains(invalidFQN)).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMergeInputSets() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputSet1 = "input1.yml";
    String inputSetYaml1 =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet1)), StandardCharsets.UTF_8);
    String inputSet2 = "input2.yml";
    String inputSetYaml2 =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSet2)), StandardCharsets.UTF_8);
    List<String> inputSetYamlList = new ArrayList<>();
    inputSetYamlList.add(inputSetYaml1);
    inputSetYamlList.add(inputSetYaml2);

    String filename = "pipeline-extensive.yml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    String templateYaml = MergeHelper.createTemplateFromPipeline(yaml, true);

    String mergedYaml = MergeHelper.mergeInputSets(templateYaml, inputSetYamlList);

    String inputSetMerged = "input12-merged.yml";
    String inputSetYamlMerged =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetMerged)), StandardCharsets.UTF_8);
    assertThat(mergedYaml).isEqualTo(inputSetYamlMerged);
  }
}