package software.wings.beans.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;

/**
 * Created by brett on 5/30/17
 */
public class InitSshCommandUnitTest extends WingsBaseTest {
  @Test
  public void testEscapifyString() {
    assertThat(InitSshCommandUnit.escapifyString("a\\a")).isEqualTo("a\\\\a");
    assertThat(InitSshCommandUnit.escapifyString("b\\\\b")).isEqualTo("b\\\\\\\\b");
    assertThat(InitSshCommandUnit.escapifyString("a$b")).isEqualTo("a\\$b");
    assertThat(InitSshCommandUnit.escapifyString("a&b")).isEqualTo("a\\&b");
    assertThat(InitSshCommandUnit.escapifyString("a\"b")).isEqualTo("a\\\"b");
    assertThat(InitSshCommandUnit.escapifyString("a'b")).isEqualTo("a\\'b");
    assertThat(InitSshCommandUnit.escapifyString("a`b")).isEqualTo("a\\`b");
    assertThat(InitSshCommandUnit.escapifyString("a(b")).isEqualTo("a\\(b");
    assertThat(InitSshCommandUnit.escapifyString("a)b")).isEqualTo("a\\)b");
    assertThat(InitSshCommandUnit.escapifyString("a|b")).isEqualTo("a\\|b");
    assertThat(InitSshCommandUnit.escapifyString("a<b")).isEqualTo("a\\<b");
    assertThat(InitSshCommandUnit.escapifyString("a>b")).isEqualTo("a\\>b");
    assertThat(InitSshCommandUnit.escapifyString("a;b")).isEqualTo("a\\;b");
  }
}
