package software.wings.beans.command;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.command.InitSshCommandUnit.escapifyString;

import org.junit.Test;
import software.wings.WingsBaseTest;

/**
 * Created by brett on 5/30/17
 */
public class InitSshCommandUnitTest extends WingsBaseTest {
  @Test
  public void testEscapifyString() {
    assertThat(escapifyString("ab\\")).isEqualTo("ab\\\\");
    assertThat(escapifyString("ab\\cd")).isEqualTo("ab\\cd");
    assertThat(escapifyString("a\"b")).isEqualTo("a\\\"b");
    assertThat(escapifyString("a'b")).isEqualTo("a'b");
    assertThat(escapifyString("a`b")).isEqualTo("a\\`b");
    assertThat(escapifyString("a(b")).isEqualTo("a(b");
    assertThat(escapifyString("a)b")).isEqualTo("a)b");
    assertThat(escapifyString("a|b")).isEqualTo("a|b");
    assertThat(escapifyString("a<b")).isEqualTo("a<b");
    assertThat(escapifyString("a>b")).isEqualTo("a>b");
    assertThat(escapifyString("a;b")).isEqualTo("a;b");
    assertThat(escapifyString("a b")).isEqualTo("a b");
  }
}
