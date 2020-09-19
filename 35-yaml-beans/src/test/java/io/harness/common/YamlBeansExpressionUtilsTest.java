package io.harness.common;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.regex.Pattern;

public class YamlBeansExpressionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMatchesInputSetPattern() {
    String notInputSet = "${inpu}.t.abc";
    String notInputSet1 = "${input.test}";
    String inputSet = "${input}";
    String inputSet1 = "${input}.allowedValues(dev, ${env}, ${env2}, stage)";
    String inputSet2 = "${input}  ";

    assertThat(YamlBeansExpressionUtils.matchesInputSetPattern(notInputSet)).isFalse();
    assertThat(YamlBeansExpressionUtils.matchesInputSetPattern(notInputSet1)).isFalse();
    assertThat(YamlBeansExpressionUtils.matchesInputSetPattern(inputSet)).isTrue();
    assertThat(YamlBeansExpressionUtils.matchesInputSetPattern(inputSet1)).isTrue();
    assertThat(YamlBeansExpressionUtils.matchesInputSetPattern(inputSet2)).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testContainsPattern() {
    String expression1 = "${input}.allowedValues(dev, ${env}, ${env2}, stage)";
    String expression2 = "${input}.allowedValues123(dev, ${env}, ${env2}, stage)";

    String pattern = YamlBeansExpressionUtils.getInputSetValidatorPattern("allowedValues");

    boolean contains = YamlBeansExpressionUtils.containsPattern(Pattern.compile(pattern), expression1);
    assertThat(contains).isTrue();
    contains = YamlBeansExpressionUtils.containsPattern(Pattern.compile(pattern), expression2);
    assertThat(contains).isFalse();
  }
}