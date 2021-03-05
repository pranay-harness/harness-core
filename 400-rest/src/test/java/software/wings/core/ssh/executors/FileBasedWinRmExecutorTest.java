package software.wings.core.ssh.executors;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.KERBEROS;

import static org.apache.commons.io.IOUtils.contentEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.ssh.SshHelperUtils;

import software.wings.beans.ConfigFile;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.utils.ExecutionLogWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnitParamsRunner.class)
@PrepareForTest({WinRmSession.class, SshHelperUtils.class})
public class FileBasedWinRmExecutorTest extends CategoryTest {
  @Mock private LogCallback logCallback;
  @Mock private DelegateFileManager delegateFileManager;
  FileBasedWinRmExecutor plainOldExecutor;
  FileBasedWinRmExecutor plainOldExecutorWithOptimization;
  FileBasedWinRmExecutor executorWithDisableEncoding;
  FileBasedWinRmExecutor executorWithDisableEncodingAndOptimization;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    final WinRmSessionConfig winRmSessionConfig =
        WinRmSessionConfig.builder().authenticationScheme(KERBEROS).username("admin").domain("domain").build();
    plainOldExecutor =
        spy(new FileBasedWinRmExecutor(logCallback, delegateFileManager, false, winRmSessionConfig, false, false));
    plainOldExecutorWithOptimization =
        spy(new FileBasedWinRmExecutor(logCallback, delegateFileManager, false, winRmSessionConfig, false, true));
    executorWithDisableEncoding =
        spy(new FileBasedWinRmExecutor(logCallback, delegateFileManager, false, winRmSessionConfig, true, false));
    executorWithDisableEncodingAndOptimization =
        spy(new FileBasedWinRmExecutor(logCallback, delegateFileManager, false, winRmSessionConfig, true, true));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  @Parameters({"1", "491", "1024", "4096", "8492", "31297"})
  public void copyConfigFilesOptimized(int size) throws IOException {
    testCopyConfigFilesForExecutor(size, plainOldExecutorWithOptimization);
    testCopyConfigFilesForExecutor(size, executorWithDisableEncodingAndOptimization);
  }

  private void testCopyConfigFilesForExecutor(int size, FileBasedWinRmExecutor executor) throws IOException {
    mockStatic(SshHelperUtils.class);
    mockRemoteCommandStatus(executor, SUCCESS);
    doReturn(buildByteInputStream(size))
        .when(delegateFileManager)
        .downloadByConfigFileId(anyString(), anyString(), anyString(), anyString());
    executor.copyConfigFiles(buildConfigFileMetadata(size));

    final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    verify(executor, atLeastOnce()).getCopyConfigCommand(any(ConfigFileMetaData.class), captor.capture());

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    captor.getAllValues().forEach(v -> {
      try {
        byteArrayOutputStream.write(v);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    assertThat(contentEquals(buildByteInputStream(size), new ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
        .isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getDeleteFileCommandBehindFF() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command = executorWithDisableEncoding.getDeleteFileCommandStr(configFileMetaData);
    assertThat(command).isEqualTo("$fileName = \"TEST_PATH\\TEST_FILE_NAME\"\n"
        + "Write-Host \"Clearing target config file $fileName on the host.\""
        + "\n[IO.File]::Delete($fileName)");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getDeleteFileCommand() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command = plainOldExecutor.getDeleteFileCommandStr(configFileMetaData);
    assertThat(command).isEqualTo("$decodedFile = 'TEST_PATH\\TEST_FILE_NAME'\n"
        + "Write-Host \"Clearing target config file $decodedFile  on the host.\"\n"
        + "[IO.File]::Delete($decodedFile)");
  }

  /**
   * Base64 encoding, no optimization. Transfer file as is using WriteAllText
   */
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCopyConfigCommandEncoded() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command = plainOldExecutor.getCopyConfigCommand(configFileMetaData, "This is a test".getBytes());
    assertThat(command).isEqualTo("#### Convert Base64 string back to config file ####\n"
        + "\n"
        + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\""
        + "VGhpcyBpcyBhIHRlc3Q="
        + "\"))\n"
        + "Write-Host \"Decoding config file on the host.\"\n"
        + "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "[IO.File]::WriteAllText($decodedFile, $DecodedString) \n"
        + "Write-Host \"Copied config file to the host.\"\n");
  }

  /**
   * Base64 encoding, split file optimization. Transfer file using AppendAllText
   */
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCopyConfigEncodedAndOptimization() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command =
        plainOldExecutorWithOptimization.getCopyConfigCommand(configFileMetaData, "This is a test".getBytes());
    assertThat(command).isEqualTo("#### Convert Base64 string back to config file ####\n"
        + "\n"
        + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\"VGhpcyBpcyBhIHRlc3Q=\"))\n"
        + "Write-Host \"Decoding config file on the host.\"\n"
        + "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "[IO.File]::AppendAllText($decodedFile, $DecodedString) \n"
        + "Write-Host \"Appended to config file on the host.\"\n");
  }

  /**
   * no encoding, split file optimization. Transfer file using AppendAllText in plaintext
   */
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCopyConfigCommandDisableEncodingAndOptimization() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command = executorWithDisableEncodingAndOptimization.getCopyConfigCommand(
        configFileMetaData, "This is a test".getBytes());
    assertThat(command).isEqualTo("$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\"\n"
        + "$commandString = @'\n" + new String("This` is` a` test".getBytes()) + "\n'@"
        + "\n[IO.File]::AppendAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
        + "Write-Host \"Appended to config file on the host.\"");
  }

  /**
   * no encoding, no optimization. Transfer file as is using WriteAllText in plaintext
   */
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCopyConfigCommandDisableEncoding() {
    ConfigFileMetaData configFileMetaData = buildConfigFileMetadata(1);
    String command = executorWithDisableEncoding.getCopyConfigCommand(configFileMetaData, "This is a test".getBytes());
    assertThat(command).isEqualTo("$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\"\n"
        + "$commandString = {" + new String("This is a test".getBytes()) + "}"
        + "\n[IO.File]::WriteAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
        + "Write-Host \"Copied config file to the host.\"\n");
  }

  private ConfigFileMetaData buildConfigFileMetadata(long size) {
    ConfigFile configFile = ConfigFile.builder().encrypted(false).entityId("TEST_ID").build();
    configFile.setSize(size);
    configFile.setUuid("fileUuid");
    return ConfigFileMetaData.builder()
        .destinationDirectoryPath("TEST_PATH")
        .fileId(configFile.getUuid())
        .filename("TEST_FILE_NAME")
        .length(configFile.getSize())
        .encrypted(configFile.isEncrypted())
        .activityId("TEST_ACTIVITY_ID")
        .build();
  }

  private InputStream buildByteInputStream(int l) {
    final byte[] bytes = new byte[l];
    Arrays.fill(bytes, (byte) 'a');
    return new ByteArrayInputStream(bytes);
  }

  private void mockRemoteCommandStatus(FileBasedWinRmExecutor executor, CommandExecutionStatus status)
      throws IOException {
    doReturn(status).when(executor).executeRemoteCommand(any(WinRmSession.class), any(ExecutionLogWriter.class),
        any(ExecutionLogWriter.class), anyString(), anyBoolean());
  }
}