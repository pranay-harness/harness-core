package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class SaveCacheStepInfoTest extends CIBeansTest {
  private String yamlString;
  @Before
  public void setUp() {
    yamlString = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("save_cache_step.yml")), "UTF-8")
                     .useDelimiter("\\A")
                     .next();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeSaveCacheStep() throws IOException {
    StepElement stepElement = YamlPipelineUtils.read(yamlString, StepElement.class);
    SaveCacheStepInfo saveCacheStepInfo = (SaveCacheStepInfo) stepElement.getStepSpecType();

    TypeInfo nonYamlInfo = saveCacheStepInfo.getNonYamlInfo();
    assertThat(nonYamlInfo.getStepInfoType()).isEqualTo(CIStepInfoType.SAVE_CACHE);

    assertThat(saveCacheStepInfo)
        .isNotNull()
        .isEqualTo(SaveCacheStepInfo.builder()
                       .identifier("cacheResults")
                       .name("stepName")
                       .key("test_results")
                       .paths(Arrays.asList("~/test_results.output", "~/test_results.error"))
                       .build());
  }
}
