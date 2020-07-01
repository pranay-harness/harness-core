package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.intfc.StepInfo;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class RestoreCacheStepInfoTest extends CIBeansTest {
  private String yamlString;
  @Before
  public void setUp() {
    yamlString = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("restore_cache_step.yml")), "UTF-8")
                     .useDelimiter("\\A")
                     .next();
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeRestoreCacheStep() throws IOException {
    StepInfo stepInfo = YamlPipelineUtils.read(yamlString, StepInfo.class);
    RestoreCacheStepInfo saveCacheStepInfo = (RestoreCacheStepInfo) stepInfo;

    TypeInfo nonYamlInfo = saveCacheStepInfo.getNonYamlInfo();
    assertThat(nonYamlInfo.getStepInfoType()).isEqualTo(CIStepInfoType.RESTORE_CACHE);

    assertThat(saveCacheStepInfo).isNotNull();
    assertThat(saveCacheStepInfo.getIdentifier()).isEqualTo("restoreCacheResults");
    assertThat(saveCacheStepInfo.getDisplayName()).isEqualTo("restore-cache-step-name");
    assertThat(saveCacheStepInfo.getRestoreCache())
        .isNotNull()
        .isEqualTo(RestoreCacheStepInfo.RestoreCache.builder().key("test_results").failIfNotExist(false).build());
  }
}