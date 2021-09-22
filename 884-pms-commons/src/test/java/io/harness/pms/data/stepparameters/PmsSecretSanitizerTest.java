package io.harness.pms.data.stepparameters;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PmsCommonsTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSecretSanitizerTest extends PmsCommonsTestBase {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testSanitize() {
    String toBeMatched = "abcd${ngSecretManager.obtain(\"var1\", 12345)}";
    assertThat(PmsSecretSanitizer.sanitize(toBeMatched)).isEqualTo("abcd*******");

    String notToBeMatched = "testing1234";
    assertThat(PmsSecretSanitizer.sanitize(notToBeMatched)).isEqualTo(notToBeMatched);
  }
}