package software.wings.core.ssh.executors;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.io.CharStreams;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.rule.Owner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ShellExecutionData;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ScriptProcessExecutorTest extends WingsBaseTest {
  @Mock private DelegateFileManager delegateFileManager;
  @Mock private DelegateLogService logService;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private ScriptProcessExecutor scriptProcessExecutor;
  private ShellExecutorConfig shellExecutorConfig;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteBashScriptWithSweepingOutputOnDelegateSuccess() {
    Map<String, String> env = new HashMap<>();
    env.put("PATH", "/Users/user/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
    shellExecutorConfig = ShellExecutorConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .appId(APP_ID)
                              .commandUnitName("MyCommand")
                              .executionId(ACTIVITY_ID)
                              .scriptType(ScriptType.BASH)
                              .workingDirectory("/tmp")
                              .environment(env)
                              .build();
    scriptProcessExecutor = new ScriptProcessExecutor(delegateFileManager, logService, shellExecutorConfig);
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    String command = "export A=\"aaa\"\n"
        + "export B=\"bbb\"";
    CommandExecutionResult commandExecutionResult =
        scriptProcessExecutor.executeCommandString(command, asList("A", "B", "${C}", "${A}"));
    assertThat(commandExecutionResult).isNotNull();
    assertThat(commandExecutionResult.getStatus()).isEqualTo(SUCCESS);
    assertThat(commandExecutionResult.getCommandExecutionData()).isNotNull();
    ShellExecutionData shellExecutionData = (ShellExecutionData) commandExecutionResult.getCommandExecutionData();
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).isNotEmpty();
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).containsEntry("A", "aaa");
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).containsEntry("B", "bbb");
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).containsKey("aaa");
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).doesNotContainKey("C");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteBashScriptWithSweepingOutputOnDelegateFails() {
    Map<String, String> env = new HashMap<>();
    env.put("PATH", "/Users/user/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
    shellExecutorConfig = ShellExecutorConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .appId(APP_ID)
                              .commandUnitName("MyCommand")
                              .executionId(ACTIVITY_ID)
                              .scriptType(ScriptType.BASH)
                              .environment(env)
                              .build();
    scriptProcessExecutor = new ScriptProcessExecutor(delegateFileManager, logService, shellExecutorConfig);
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    String command = "exit 1";
    CommandExecutionResult commandExecutionResult =
        scriptProcessExecutor.executeCommandString(command, asList("A", "B"));
    assertThat(commandExecutionResult).isNotNull();
    assertThat(commandExecutionResult.getStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(commandExecutionResult.getCommandExecutionData()).isNotNull();
    ShellExecutionData shellExecutionData = (ShellExecutionData) commandExecutionResult.getCommandExecutionData();
    assertThat(shellExecutionData.getSweepingOutputEnvVariables()).isEmpty();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteBashScriptWithWorkingDirectoryOnDelegateSuccess() {
    Map<String, String> env = new HashMap<>();
    env.put("PATH", "/Users/user/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
    shellExecutorConfig = ShellExecutorConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .appId(APP_ID)
                              .commandUnitName("MyCommand")
                              .executionId(ACTIVITY_ID)
                              .scriptType(ScriptType.BASH)
                              .workingDirectory("/tmp")
                              .environment(env)
                              .build();
    scriptProcessExecutor = new ScriptProcessExecutor(delegateFileManager, logService, shellExecutorConfig);
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    String command = "export A=\"aaa\"\n"
        + "export B=\"bbb\"";
    CommandExecutionStatus commandExecutionStatus = scriptProcessExecutor.executeCommandString(command, null, true);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testExecuteBashScriptWithoutWorkingDirectoryOnDelegateSuccess() {
    Map<String, String> env = new HashMap<>();
    env.put("PATH", "/Users/user/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin");
    shellExecutorConfig = ShellExecutorConfig.builder()
                              .accountId(ACCOUNT_ID)
                              .appId(APP_ID)
                              .commandUnitName("MyCommand")
                              .executionId(ACTIVITY_ID)
                              .scriptType(ScriptType.BASH)
                              .environment(env)
                              .build();
    scriptProcessExecutor = new ScriptProcessExecutor(delegateFileManager, logService, shellExecutorConfig);
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    String command = "export A=\"aaa\"\n"
        + "export B=\"bbb\"";
    CommandExecutionStatus commandExecutionStatus = scriptProcessExecutor.executeCommandString(command, null, true);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testScpOneFileSuccess() throws IOException {
    scriptProcessExecutor =
        new ScriptProcessExecutor(delegateFileManager, logService, ShellExecutorConfig.builder().build());
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    AbstractScriptExecutor.FileProvider fileProvider = new AbstractScriptExecutor.FileProvider() {
      @Override
      public Pair<String, Long> getInfo() throws IOException {
        File file1 = new File(file.getAbsolutePath());
        return ImmutablePair.of(file1.getName(), file1.length());
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
          IOUtils.copy(fis, outputStream);
        }
      }
    };
    CommandExecutionStatus commandExecutionStatus = scriptProcessExecutor.scpOneFile("/tmp", fileProvider);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
    File tempFile = new File("/tmp/" + fileProvider.getInfo().getKey());
    boolean exists = tempFile.exists();
    assertThat(exists).isTrue();

    // cleanup
    deleteFileIfExists(tempFile.getAbsolutePath());
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testScpOneFileFails() {
    scriptProcessExecutor =
        new ScriptProcessExecutor(delegateFileManager, logService, ShellExecutorConfig.builder().build());
    on(scriptProcessExecutor).set("logService", logService);
    on(scriptProcessExecutor).set("delegateFileManager", delegateFileManager);

    AbstractScriptExecutor.FileProvider fileProvider = new AbstractScriptExecutor.FileProvider() {
      @Override
      public Pair<String, Long> getInfo() throws IOException {
        return ImmutablePair.of(null, 0L);
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws IOException {
        try (FileInputStream fis = new FileInputStream("")) {
          IOUtils.copy(fis, outputStream);
        }
      }
    };
    CommandExecutionStatus commandExecutionStatus = scriptProcessExecutor.scpOneFile("/randomdir", fileProvider);
    assertThat(commandExecutionStatus).isEqualTo(FAILURE);
  }
}
