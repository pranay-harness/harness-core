package software.wings.core.winrm.executors;

import static io.harness.rule.OwnerRule.DINESH;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import java.util.List;

public class DefaultWinRmExecutorTest extends CategoryTest {
  @Mock DefaultWinRmExecutor defaultWinRmExecutor;
  @Mock DelegateLogService logService;
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
  String simpleCommand, reallyLongCommand;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    spyDefaultWinRmExecutor = new DefaultWinRmExecutor(logService, delegateFileManager, config, true);
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
    doReturn(CommandExecutionStatus.SUCCESS).when(defaultWinRmExecutor).copyConfigFiles(configFileMetaData);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testConstructPSScriptWithCommands() {
    List<List<String>> result1 =
        spyDefaultWinRmExecutor.constructPSScriptWithCommands(simpleCommand, "tempPSScript.ps1");
    Assertions.assertThat(result1.size()).isEqualTo(1);

    List<List<String>> result2 =
        spyDefaultWinRmExecutor.constructPSScriptWithCommands(reallyLongCommand, "tempPSScript.ps1");
    Assertions.assertThat(result2.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testCleanUpFilesDisableEncodingFFOn() {
    DefaultWinRmExecutor defaultWinRmExecutorFFOn =
        new DefaultWinRmExecutor(logService, delegateFileManager, config, true);
    defaultWinRmExecutorFFOn.cleanupFiles(winRmSession, "PSFileName.ps1");
    verify(winRmSession, times(1)).executeCommandString(any(), any(), any());
  }
}
