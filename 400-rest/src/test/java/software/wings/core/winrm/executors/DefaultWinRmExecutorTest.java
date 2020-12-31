package software.wings.core.winrm.executors;

import static io.harness.rule.OwnerRule.DINESH;
import static io.harness.rule.OwnerRule.INDER;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.beans.ConfigFile;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.core.ssh.executors.FileBasedWinRmExecutor;
import software.wings.core.ssh.executors.WinRmExecutorHelper;
import software.wings.delegatetasks.DelegateFileManager;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultWinRmExecutorTest extends CategoryTest {
  @Mock DefaultWinRmExecutor defaultWinRmExecutor;
  @Mock FileBasedWinRmExecutor fileBasedWinRmExecutor;
  @Mock LogCallback logCallback;
  @Mock WinRmSessionConfig config;
  @Mock DelegateFileManager delegateFileManager;
  @Mock WinRmSession winRmSession;
  private ConfigFile configFile = ConfigFile.builder().encrypted(false).entityId("TEST_ID").build();
  private ConfigFileMetaData configFileMetaData = ConfigFileMetaData.builder()
                                                      .destinationDirectoryPath("TEST_PATH")
                                                      .fileId(configFile.getUuid())
                                                      .filename("TEST_FILE_NAME")
                                                      .length(configFile.getSize())
                                                      .encrypted(configFile.isEncrypted())
                                                      .activityId("TEST_ACTIVITY_ID")
                                                      .build();

  private DefaultWinRmExecutor spyDefaultWinRmExecutor;
  private FileBasedWinRmExecutor spyFileBasedWinRmExecutor;
  String simpleCommand, reallyLongCommand;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    spyDefaultWinRmExecutor = new DefaultWinRmExecutor(logCallback, delegateFileManager, true, config, true);
    spyFileBasedWinRmExecutor = new FileBasedWinRmExecutor(logCallback, delegateFileManager, true, config, true);
    simpleCommand = "$test=\"someruntimepath\"\n"
        + "echo $test\n"
        + "if($test){\n"
        + "    Write-Host \"i am inside if\"\n"
        + "} else {\n"
        + "    Write-Host \"i am inside else\"\n"
        + "}";

    reallyLongCommand = simpleCommand + simpleCommand + simpleCommand + simpleCommand
        + "$myfile = Get-Content -Path \"C:\\Users\\rohit_karelia\\logontest.ps1\" | Get-Unique | Measure-Object \n"
        + "echo $myfile";
  }

  @Test
  @Owner(developers = DINESH)
  @Category(UnitTests.class)
  public void shouldCopyConfigFile() {
    doReturn(CommandExecutionStatus.SUCCESS).when(fileBasedWinRmExecutor).copyConfigFiles(configFileMetaData);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testConstructPSScriptWithCommands() {
    List<List<String>> result1 = WinRmExecutorHelper.constructPSScriptWithCommands(
        simpleCommand, "tempPSScript.ps1", DefaultWinRmExecutor.POWERSHELL);
    assertThat(result1.size()).isEqualTo(1);

    List<List<String>> result2 = WinRmExecutorHelper.constructPSScriptWithCommands(
        reallyLongCommand, "tempPSScript.ps1", DefaultWinRmExecutor.POWERSHELL);
    assertThat(result2.size()).isEqualTo(2);
    verify(config, times(2)).isUseNoProfile();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConstructPSScriptWithCommandsWithoutProfile() {
    when(config.isUseNoProfile()).thenReturn(true);
    List<List<String>> result1 = WinRmExecutorHelper.constructPSScriptWithCommands(
        simpleCommand, "tempPSScript.ps1", DefaultWinRmExecutor.POWERSHELL);
    assertThat(result1.size()).isEqualTo(1);

    List<List<String>> result2 = WinRmExecutorHelper.constructPSScriptWithCommands(
        reallyLongCommand, "tempPSScript.ps1", DefaultWinRmExecutor.POWERSHELL);
    assertThat(result2.size()).isEqualTo(2);

    verify(config, times(2)).isUseNoProfile();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testCleanUpFilesDisableEncodingFFOn() {
    DefaultWinRmExecutor defaultWinRmExecutorFFOn =
        new DefaultWinRmExecutor(logCallback, delegateFileManager, true, config, true);
    WinRmExecutorHelper.cleanupFiles(winRmSession, "PSFileName.ps1", DefaultWinRmExecutor.POWERSHELL, true);
    verify(winRmSession, times(1)).executeCommandString(any(), any(), any(), eq(false));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testpsWrappedCommandWithEncodingWithProfile() {
    when(config.isUseNoProfile()).thenReturn(true);
    spyDefaultWinRmExecutor = new DefaultWinRmExecutor(logCallback, delegateFileManager, true, config, true);
    String poweshellCommand =
        WinRmExecutorHelper.psWrappedCommandWithEncoding(simpleCommand, DefaultWinRmExecutor.POWERSHELL_NO_PROFILE);
    assertThat(poweshellCommand.contains("NoProfile")).isTrue();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testpsWrappedCommandWithEncodingWithoutProfile() {
    when(config.isUseNoProfile()).thenReturn(false);
    String poweshellCommand =
        WinRmExecutorHelper.psWrappedCommandWithEncoding(simpleCommand, DefaultWinRmExecutor.POWERSHELL);
    assertThat(poweshellCommand.contains("NoProfile")).isFalse();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCopyFiles() {
    assertThatThrownBy(() -> spyFileBasedWinRmExecutor.copyFiles("", new ArrayList<>()))
        .isInstanceOf(NotImplementedException.class)
        .hasMessageContaining(DefaultWinRmExecutor.NOT_IMPLEMENTED);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCopyConfigCommand() {
    String command = spyFileBasedWinRmExecutor.getCopyConfigCommand(configFileMetaData, "This is a test");
    assertThat(command).isEqualTo("#### Convert Base64 string back to config file ####\n"
        + "\n"
        + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\""
        + "This is a test"
        + "\"))\n"
        + "Write-Host \"Decoding config file on the host.\"\n"
        + "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "[IO.File]::WriteAllText($decodedFile, $DecodedString) \n"
        + "Write-Host \"Copied config file to the host.\"\n");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCopyConfigCommandBehindFF() {
    String command =
        spyFileBasedWinRmExecutor.getCopyConfigCommandBehindFF(configFileMetaData, "This is a test".getBytes());
    assertThat(command).isEqualTo("$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\"\n"
        + "$commandString = {" + new String("This is a test".getBytes()) + "}"
        + "\n[IO.File]::WriteAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
        + "Write-Host \"Copied config file to the host.\"\n");
  }
}
