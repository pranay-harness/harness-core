package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.CiBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RestoreCacheStepInfoTest extends CiBeansTestBase {
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
    //    StepElement stepElement = YamlPipelineUtils.read(yamlString, StepElement.class);
    //  RestoreCacheStepInfo restoreCacheStepInfo = (RestoreCacheStepInfo) stepElement.getStepSpecType();

    //    TypeInfo nonYamlInfo = restoreCacheStepInfo.getNonYamlInfo();
    //    assertThat(nonYamlInfo.getStepInfoType()).isEqualTo(CIStepInfoType.RESTORE_CACHE);
    //
    //    assertThat(restoreCacheStepInfo)
    //        .isNotNull()
    //        .isEqualTo(RestoreCacheStepInfo.builder()
    //                       .identifier("restoreCacheResults")
    //                       .name("restore-cache-step-name")
    //                       .key("test_results")
    //                       .build());
  }
}
