package software.wings.core.winrm.executors;

import static org.mockito.Mockito.doReturn;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;

public class DefaultWinRmExecutorTest {
  @Mock DefaultWinRmExecutor defaultWinRmExecutor;
  private ConfigFile configFile = ConfigFile.builder().encrypted(false).entityId("TEST_ID").build();
  private ConfigFileMetaData configFileMetaData = ConfigFileMetaData.builder()
                                                      .destinationDirectoryPath("TEST_PATH")
                                                      .fileId(configFile.getUuid())
                                                      .filename("TEST_FILE_NAME")
                                                      .length(configFile.getSize())
                                                      .encrypted(configFile.isEncrypted())
                                                      .activityId("TEST_ACTIVITY_ID")
                                                      .build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldCopyConfigFile() {
    doReturn(CommandExecutionStatus.SUCCESS).when(defaultWinRmExecutor).copyConfigFiles(configFileMetaData);
  }
}
