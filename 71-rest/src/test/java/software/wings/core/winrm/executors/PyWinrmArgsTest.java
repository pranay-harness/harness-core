package software.wings.core.winrm.executors;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PyWinrmArgsTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getArgs() {
    Map<String, String> m = new HashMap<>();
    m.put("k1", "v1");
    m.put("k2", ",=v2=,==j");
    m.put("VARIA'BLE_N?AME_F'OO", "\"SOME_%$!_VA'LUE@?=A");
    PyWinrmArgs args = PyWinrmArgs.builder()
                           .username("harness")
                           .hostname("host")
                           .workingDir("/temp")
                           .timeout(10)
                           .serverCertValidation(true)
                           .environmentMap(m)
                           .build();
    Assertions.assertThat(args.getArgs("/commandFile.path"))
        .isEqualTo(
            "-e 'host' -u 'harness' -s 'true' -env $'k1=v1' $'k2=,=v2=,==j' $'VARIA\\'BLE_N?AME_F\\'OO=\"SOME_%$!_VA\\'LUE@?=A' -w '/temp' -t '10' -cfile '/commandFile.path'");
  }

  @Test
  @Owner(developers = OwnerRule.SAHIL)
  @Category(UnitTests.class)
  public void getArgsEscapedValueEnvMap() {
    Map<String, String> m = new HashMap<>();
    m.put("k1", "v1");
    m.put("k2", ",=v2=,==j' now escape quote \\' now double quote \\\\\\' now ends with \\");
    m.put("VARIA'BLE_N?AME_F'OO", "\"SOME_%$!_VA'LUE@?=A");
    PyWinrmArgs args = PyWinrmArgs.builder()
                           .username("harness")
                           .hostname("host")
                           .workingDir("/temp")
                           .timeout(10)
                           .serverCertValidation(true)
                           .environmentMap(m)
                           .build();
    Assertions.assertThat(args.getArgs("/commandFile.path"))
        .isEqualTo(
            "-e 'host' -u 'harness' -s 'true' -env $'k1=v1' $'k2=,=v2=,==j\\' now escape quote \\' now double quote \\\\\\' now ends with \\\\' $'VARIA\\'BLE_N?AME_F\\'OO=\"SOME_%$!_VA\\'LUE@?=A' -w '/temp' -t '10' -cfile '/commandFile.path'");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getArgsEmptyEnvMap() {
    Map<String, String> m = new HashMap<>();
    PyWinrmArgs args = PyWinrmArgs.builder()
                           .username("harness")
                           .hostname("host")
                           .workingDir("/temp")
                           .timeout(10)
                           .serverCertValidation(true)
                           .environmentMap(m)
                           .build();
    Assertions.assertThat(args.getArgs("/commandFile.path"))
        .isEqualTo("-e 'host' -u 'harness' -s 'true' -env {} -w '/temp' -t '10' -cfile '/commandFile.path'");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getArgsNullEnvMap() {
    PyWinrmArgs args = PyWinrmArgs.builder()
                           .username("harness")
                           .hostname("host")
                           .workingDir("/temp")
                           .timeout(10)
                           .serverCertValidation(true)
                           .environmentMap(null)
                           .build();
    Assertions.assertThat(args.getArgs("/commandFile.path"))
        .isEqualTo("-e 'host' -u 'harness' -s 'true' -env {} -w '/temp' -t '10' -cfile '/commandFile.path'");
  }
}
