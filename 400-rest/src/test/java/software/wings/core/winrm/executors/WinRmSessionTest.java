package software.wings.core.winrm.executors;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import io.harness.category.element.UnitTests;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.ssh.SshHelperUtils;

import software.wings.WingsBaseTest;
import software.wings.beans.WinRmConnectionAttributes;

import com.jcraft.jsch.JSchException;
import java.io.Writer;
import java.util.HashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({software.wings.utils.SshHelperUtils.class, io.harness.ssh.SshHelperUtils.class, WinRmSession.class,
    InstallUtils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class WinRmSessionTest extends WingsBaseTest {
  @Mock private SshHelperUtils sshHelperUtils;
  @Mock private Writer writer;
  @Mock private Writer error;
  @Mock private LogCallback logCallback;

  private WinRmSessionConfig winRmSessionConfig;

  private WinRmSession winRmSession;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecuteCommandString() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);

    int status = winRmSession.executeCommandString("ls", writer, error, false);

    PowerMockito.verifyStatic(io.harness.ssh.SshHelperUtils.class);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    SshHelperUtils.generateTGT(captor.capture(), anyString(), anyString(), eq(logCallback));
    assertThat(captor.getValue()).isEqualTo("TestUser@KRB.LOCAL");
    SshHelperUtils.executeLocalCommand(anyString(), eq(logCallback), eq(writer), eq(false));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUserPrincipal() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);

    String userPrincipal = winRmSession.getUserPrincipal("test", "domain");

    assertThat(userPrincipal).isEqualTo("test@DOMAIN");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUserPrincipalWithDomainInUsername() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);

    String userPrincipal = winRmSession.getUserPrincipal("test@oldDomain", "domain");

    assertThat(userPrincipal).isEqualTo("test@DOMAIN");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUserPrincipalWithUsernameNull() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> new WinRmSession(winRmSessionConfig, logCallback))
        .withMessageContaining("Username or domain cannot be null");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUserPrincipalWithDomainNull() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> new WinRmSession(winRmSessionConfig, logCallback))
        .withMessageContaining("Username or domain cannot be null");
  }
}
