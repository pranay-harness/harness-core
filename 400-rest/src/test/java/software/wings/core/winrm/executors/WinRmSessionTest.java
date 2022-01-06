/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.core.winrm.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.ssh.SshHelperUtils;

import software.wings.beans.WinRmConnectionAttributes;

import com.jcraft.jsch.JSchException;
import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientBuilder;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({software.wings.utils.SshHelperUtils.class, io.harness.ssh.SshHelperUtils.class, WinRmSession.class,
    InstallUtils.class, WinRmClient.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class WinRmSessionTest extends CategoryTest {
  @Mock private SshHelperUtils sshHelperUtils;
  @Mock private Writer writer;
  @Mock private Writer error;
  @Mock private LogCallback logCallback;

  private WinRmSessionConfig winRmSessionConfig;

  private WinRmSession winRmSession;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

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
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);

    int status = winRmSession.executeCommandString("ls", writer, error, false);

    PowerMockito.verifyStatic(io.harness.ssh.SshHelperUtils.class);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    SshHelperUtils.generateTGT(captor.capture(), anyString(), anyString(), eq(logCallback));
    assertThat(captor.getValue()).isEqualTo("TestUser@KRB.LOCAL");
    SshHelperUtils.executeLocalCommand(anyString(), eq(logCallback), eq(writer), eq(false));
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testRemoteHost() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testRemoteHostCheckNegativeCase() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(false);
    try {
      winRmSession = new WinRmSession(winRmSessionConfig, logCallback);
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage()).isEqualTo("Cannot reach remote host");
    }
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
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
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
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);

    String userPrincipal = winRmSession.getUserPrincipal("test@oldDomain", "domain");

    assertThat(userPrincipal).isEqualTo("test@DOMAIN");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUserPrincipalWithUsernameNull() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
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
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
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

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testAutoClosable() throws JSchException {
    PowerMockito.mockStatic(SshHelperUtils.class);
    PowerMockito
        .when(SshHelperUtils.executeLocalCommand(anyString(), any(LogCallback.class), any(Writer.class), anyBoolean()))
        .thenReturn(true);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .domain("KRB.LOCAL")
                             .skipCertChecks(true)
                             .username("TestUser")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.KERBEROS)
                             .build();
    WinRmSession session = new WinRmSession(winRmSessionConfig, logCallback);
    WinRmClientContext context = Mockito.mock(WinRmClientContext.class);
    WinRmClient client = Mockito.mock(WinRmClient.class);
    on(session).set("context", context);
    on(session).set("client", client);
    session.close();
    verify(context).shutdown();
    verify(client).close();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteCommandsListWithScriptExecCommandKerberos() throws JSchException, IOException {
    List<List<String>> commandsList = new ArrayList<>();
    List<String> commands = new ArrayList<>();
    commands.add("command");
    commandsList.add(commands);
    ShellCommand shell = mock(ShellCommand.class);
    WinRmTool winRmTool = mock(WinRmTool.class);
    PyWinrmArgs pyWinrmArgs = mock(PyWinrmArgs.class);
    setupMocks(commands, shell, winRmTool, pyWinrmArgs);

    winRmSession.executeCommandsList(commandsList, writer, error, false, "executeCommand");
    assertThat(commandsList.get(commandsList.size() - 1).get(1)).isEqualTo("executeCommand");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteCommandsListWithScriptExecCommand() throws JSchException, IOException {
    List<List<String>> commandsList = new ArrayList<>();
    List<String> commands = Collections.singletonList("command");
    commandsList.add(commands);
    ShellCommand shell = mock(ShellCommand.class);
    WinRmTool winRmTool = mock(WinRmTool.class);

    setupMocks(commands, shell, winRmTool, null);

    winRmSession.executeCommandsList(commandsList, writer, error, false, "executeCommand");
    verify(winRmTool).executeCommand(commands);
    verify(shell).execute("executeCommand", writer, error);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteCommandsListWithoutScriptExecCommand() throws JSchException, IOException {
    List<List<String>> commandsList = new ArrayList<>();
    List<String> commands = Collections.singletonList("command");
    commandsList.add(commands);
    ShellCommand shell = mock(ShellCommand.class);
    WinRmTool winRmTool = mock(WinRmTool.class);

    setupMocks(commands, shell, winRmTool, null);

    winRmSession.executeCommandsList(commandsList, writer, error, false, null);
    verify(winRmTool).executeCommand(commands);
    verifyZeroInteractions(shell);
  }

  private void setupMocks(List<String> commands, ShellCommand shell, WinRmTool winRmTool, PyWinrmArgs pyWinrmArgs)
      throws JSchException {
    WinRmClientBuilder winRmClientBuilder = spy(WinRmClient.builder("http://localhost"));
    PowerMockito.mockStatic(SshHelperUtils.class);
    PowerMockito.mockStatic(WinRmClient.class);
    WinRmClient winRmClient = mock(WinRmClient.class);

    when(WinRmClient.builder(anyString())).thenReturn(winRmClientBuilder);
    doReturn(winRmClient).when(winRmClientBuilder).build();
    when(winRmClient.createShell()).thenReturn(shell);
    winRmSessionConfig = WinRmSessionConfig.builder()
                             .skipCertChecks(true)
                             .username("TestUser")
                             .password("password")
                             .environment(new HashMap<>())
                             .hostname("localhost")
                             .workingDirectory("workingDirectory")
                             .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.NTLM)
                             .build();
    winRmSession = new WinRmSession(winRmSessionConfig, logCallback);
    on(winRmSession).set("args", pyWinrmArgs);
    on(winRmSession).set("shell", shell);
    on(winRmSession).set("winRmTool", winRmTool);
    when(winRmTool.executeCommand(commands)).thenReturn(new WinRmToolResponse("", "", 0));
  }
}
