package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.common.NGExpressionUtils.GENERIC_EXPRESSIONS_PATTERN;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class NGExpressionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMatchesInputSetPattern() {
    String notInputSet = "${inpu}.t.abc";
    String notInputSet1 = "${input.test}";
    String inputSet = "<+input>";
    String inputSet1 = "<+input>.allowedValues(dev, ${env}, ${env2}, stage)";
    String inputSet2 = "<+input>  ";

    assertThat(NGExpressionUtils.matchesInputSetPattern(notInputSet)).isFalse();
    assertThat(NGExpressionUtils.matchesInputSetPattern(notInputSet1)).isFalse();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet)).isTrue();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet1)).isTrue();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet2)).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testContainsPattern() {
    String expression1 = "<+input>.allowedValues(dev, ${env}, ${env2}, stage)";
    String expression2 = "<+input>.allowedValues123(dev, ${env}, ${env2}, stage)";

    String pattern = NGExpressionUtils.getInputSetValidatorPattern("allowedValues");

    boolean contains = NGExpressionUtils.containsPattern(Pattern.compile(pattern), expression1);
    assertThat(contains).isTrue();
    contains = NGExpressionUtils.containsPattern(Pattern.compile(pattern), expression2);
    assertThat(contains).isFalse();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMatchesPattern() {
    String expression = "<+input>.allowedValues(abc)";
    String pattern1 = "<\\+input>.*";
    String pattern2 = "<\\+input>";

    boolean matchesPattern = NGExpressionUtils.matchesPattern(Pattern.compile(pattern1), expression);
    assertThat(matchesPattern).isTrue();
    matchesPattern = NGExpressionUtils.matchesPattern(Pattern.compile(pattern2), expression);
    assertThat(matchesPattern).isFalse();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testMatchesExpressionPattern() {
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+pipeline.stages.s1>")).isTrue();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+PIPEline.stages.11>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+pipeline.stages.1s>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+pipeline>")).isTrue();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+12.12.23>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+pipeline.1stages.S0OS>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+too..many.dots>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+too.many.dots.>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+.too.many.dots>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+input>")).isTrue();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+.input>")).isFalse();
    assertThat(NGExpressionUtils.containsPattern(GENERIC_EXPRESSIONS_PATTERN, "<+manifests.m2.store>")).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetListOfExpressions() {
    String complexString = "echo <+pipeline.stages.s1.description>\n"
        + "echo <+pipeline.stages.s1.description>\n"
        + "echo <+pipeline.stages.s2.description>\n"
        + "echo <+stages.s2.description>\n"
        + "echo <+stage.serviceConfig.serviceRef>\n"
        + "echo <+stage.serviceConfig.tag1>\n"
        + "echo <+input>\n"
        + "echo <+stage..input>\n"
        + "echo <+stage.input.>\n"
        + "ls .*\n"
        + "cd /Users/username/<+pipeline.stages.stage.name>/service/<+stage.serviceConfig.serviceRef>\n";
    List<String> listOfExpressions = NGExpressionUtils.getListOfExpressions(complexString);
    assertThat(listOfExpressions).hasSize(9);
    assertThat(listOfExpressions.get(0)).isEqualTo("<+pipeline.stages.s1.description>");
    assertThat(listOfExpressions.get(1)).isEqualTo("<+pipeline.stages.s1.description>");
    assertThat(listOfExpressions.get(2)).isEqualTo("<+pipeline.stages.s2.description>");
    assertThat(listOfExpressions.get(3)).isEqualTo("<+stages.s2.description>");
    assertThat(listOfExpressions.get(4)).isEqualTo("<+stage.serviceConfig.serviceRef>");
    assertThat(listOfExpressions.get(5)).isEqualTo("<+stage.serviceConfig.tag1>");
    assertThat(listOfExpressions.get(6)).isEqualTo("<+input>");
    assertThat(listOfExpressions.get(7)).isEqualTo("<+pipeline.stages.stage.name>");
    assertThat(listOfExpressions.get(8)).isEqualTo("<+stage.serviceConfig.serviceRef>");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetFirstKeyOfExpression() {
    assertThat(NGExpressionUtils.getFirstKeyOfExpression("<+pipeline.stages.s1.name>")).isEqualTo("pipeline");
    assertThat(NGExpressionUtils.getFirstKeyOfExpression("<+stages.s1.description>")).isEqualTo("stages");
    assertThat(NGExpressionUtils.getFirstKeyOfExpression("<+input>")).isEqualTo("input");
    assertThat(NGExpressionUtils.getFirstKeyOfExpression("<+step.name>")).isEqualTo("step");
    assertThat(NGExpressionUtils.getFirstKeyOfExpression("<+artifact.image>")).isEqualTo("artifact");
    assertThatThrownBy(() -> NGExpressionUtils.getFirstKeyOfExpression("staticValue"))
        .hasMessage("staticValue is not a syntactically valid pipeline expression")
        .isInstanceOf(InvalidRequestException.class);
  }
}
