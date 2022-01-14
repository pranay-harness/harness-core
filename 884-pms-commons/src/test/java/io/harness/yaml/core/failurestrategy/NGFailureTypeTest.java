package io.harness.yaml.core.failurestrategy;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.rule.Owner;

import java.util.EnumSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGFailureTypeTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNGFailureTypes() {
    EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
    for (NGFailureType ngFailureType : NGFailureType.values()) {
      assertThat(ngFailureType.getFailureTypes()).isNotEmpty();
      assertThat(ngFailureType.getYamlName()).isNotBlank();
      assertThat(NGFailureType.getFailureTypes(ngFailureType.getYamlName())).isEqualTo(ngFailureType);
      failureTypes.addAll(ngFailureType.getFailureTypes());
    }

    assertThat(NGFailureType.getAllFailureTypes()).isEqualTo(failureTypes);
    assertThatThrownBy(() -> NGFailureType.getFailureTypes("random_123")).isInstanceOf(IllegalArgumentException.class);
  }
}
