package io.harness.pms.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FQNUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGenerateFQNMap() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    List<String> filenames = Arrays.asList("test1.yaml", "test2.yaml", "test3.yaml", "test4.yaml");
    List<String> resFiles = Arrays.asList("res1.txt", "res2.txt", "res3.txt", "res4.txt");

    for (int i = 0; i < 4; i++) {
      String filename = filenames.get(i);
      String yaml =
          Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
      PipelineYamlConfig config = new PipelineYamlConfig(yaml);
      List<String> keys = config.getFqnToValueMap().keySet().stream().map(FQN::display).collect(Collectors.toList());

      String resFile = resFiles.get(i);
      String res = Resources.toString(Objects.requireNonNull(classLoader.getResource(resFile)), StandardCharsets.UTF_8);
      List<String> resKeys = YamlUtils.read(res, ArrayList.class);
      assertThat(keys).isEqualTo(resKeys);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGenerateYamlMap() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    List<String> filenames = Arrays.asList("test1.yaml", "test2.yaml", "test3.yaml", "test4.yaml");
    for (int i = 0; i < 4; i++) {
      String filename = filenames.get(i);
      String yaml =
          Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
      PipelineYamlConfig config = new PipelineYamlConfig(yaml);

      PipelineYamlConfig reverseConfig = new PipelineYamlConfig(config.getFqnToValueMap(), config.getYamlMap());
      assertThat(reverseConfig.getYaml().replace("\"", "")).isEqualTo(yaml);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGenerateFQNAndYamlMapOnFailureStrategiesYaml() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "failure-strategy.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    PipelineYamlConfig config = new PipelineYamlConfig(yaml);
    assertThat(config).isNotNull();
    PipelineYamlConfig configRes = new PipelineYamlConfig(config.getFqnToValueMap(), config.getYamlMap());
    assertThat(configRes.getYaml().replace("\"", "")).isEqualTo(yaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGenerateFQNAndYamlMapOnCIPipelineWithReports() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ci-pipeline-with-reports.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    PipelineYamlConfig config = new PipelineYamlConfig(yaml);
    assertThat(config).isNotNull();
    PipelineYamlConfig configRes = new PipelineYamlConfig(config.getFqnToValueMap(), config.getYamlMap());
    assertThat(configRes.getYaml().replace("\"", "")).isEqualTo(yaml);
  }

  private String toYaml(String fileName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    return Resources.toString(Objects.requireNonNull(classLoader.getResource(fileName)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testUniqueFQN() throws IOException {
    String ValidFqnYaml = "fqnUniqueTest1.yaml";
    String yaml1 = toYaml(ValidFqnYaml);
    assertThat(FQNUtils.generateFQNMap(YamlUtils.readTree(yaml1).getNode().getCurrJsonNode()))
        .isNotInstanceOf(InvalidRequestException.class);

    String sameVariableYaml = "fqnUniqueSameVariableNameTest.yaml";
    String yaml2 = toYaml(sameVariableYaml);
    assertThatThrownBy(() -> FQNUtils.generateFQNMap(YamlUtils.readTree(yaml2).getNode().getCurrJsonNode()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(" This element is coming twice in yaml a.d.h.[name:name1]");

    String sameStageIdentifierYaml = "fqnUniqueSameStageIdentifierTest.yaml";
    String yaml3 = toYaml(sameStageIdentifierYaml);
    assertThatThrownBy(() -> FQNUtils.generateFQNMap(YamlUtils.readTree(yaml3).getNode().getCurrJsonNode()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(" This element is coming twice in yaml a.d.stage[identifier:id1]");

    String sameBarrierIdentifierYaml = "fqnUniqueSameBarrierIdentifierTest.yaml";
    String yaml4 = toYaml(sameBarrierIdentifierYaml);
    assertThatThrownBy(() -> FQNUtils.generateFQNMap(YamlUtils.readTree(yaml4).getNode().getCurrJsonNode()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            " This element is coming twice in yaml a.d.stage[identifier:id1].h.execution.step[identifier:barr1]");

    String failureStrategyYaml = "fqnUniqueFailureStrategyTest.yaml";
    String yaml5 = toYaml(failureStrategyYaml);
    assertThat(FQNUtils.generateFQNMap(YamlUtils.readTree(yaml5).getNode().getCurrJsonNode()))
        .isNotInstanceOf(InvalidRequestException.class);
  }
}