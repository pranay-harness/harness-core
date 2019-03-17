package io.harness.windows;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CmdUtilsTest {
  @Test
  @Category(UnitTests.class)
  public void testEscapeEnvironmentValue() {
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path")).isEqualTo("path");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path>")).isEqualTo("path^>");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path<")).isEqualTo("path^<");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path^1")).isEqualTo("path^^1");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path^1<")).isEqualTo("path^^1^<");
  }

  @Test
  @Category(UnitTests.class)
  public void testEscapeEnvironmentValueWithPercentage() {
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%path%")).isEqualTo("^%path^%");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%path%me")).isEqualTo("^%path^%me");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%path%%path%")).isEqualTo("^%path^%^%path^%");

    // Invalids
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%path")).isEqualTo("");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path%")).isEqualTo("");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%%path")).isEqualTo("");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("path%%path")).isEqualTo("");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("%pa%th%")).isEqualTo("");
    assertThat(CmdUtils.escapeEnvValueSpecialChars("4dGl$KJA%%cC2*XY&amp;rK5Nv2!B6p!wj")).isEqualTo("");
  }
}
