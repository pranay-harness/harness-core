package io.harness.pms.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
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
}