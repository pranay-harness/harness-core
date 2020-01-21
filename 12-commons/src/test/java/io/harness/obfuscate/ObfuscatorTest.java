package io.harness.obfuscate;

import static io.harness.obfuscate.Obfuscator.obfuscate;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ObfuscatorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testObfuscate() {
    assertThat(obfuscate("test")).isEqualTo("098f6bcd4621d373cade4e832627b4f6");
  }
}
