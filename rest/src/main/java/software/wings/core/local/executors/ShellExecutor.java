package software.wings.core.local.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.api.ScriptType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ShellExecutor {
  private DelegateLogService logService;
  private ShellExecutorConfig config;
  private ScriptType scriptType;

  private static final String defaultParentWorkingDirectory = "./local-scripts/";
  static {
    try {
      if (!Files.exists(Paths.get(defaultParentWorkingDirectory))) {
        Files.createDirectory(Paths.get(defaultParentWorkingDirectory));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create local-scripts directory", e);
    }
  }

  ShellExecutor(DelegateLogService logService, ShellExecutorConfig config, ScriptType scriptType) {
    this.logService = logService;
    this.config = config;
    this.scriptType = scriptType;
  }

  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null);
  }

  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    saveExecutionLog(format("Executing command ..."), INFO);

    switch (this.scriptType) {
      case BASH:
        try {
          commandExecutionStatus = executeBashScript(command);
        } catch (Exception e) {
          saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
        }
        break;

      case POWERSHELL:
        commandExecutionStatus = executePowerShellScript(command);
        break;

      default:
        unhandled(this.scriptType);
    }

    return commandExecutionStatus;
  }

  private CommandExecutionStatus executeBashScript(String command) throws IOException {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    File workingDirectory;

    if (isEmpty(config.getWorkingDirectory())) {
      String directoryPath = defaultParentWorkingDirectory + config.getExecutionId();
      Files.createDirectory(Paths.get(directoryPath));
      workingDirectory = new File(directoryPath);
    } else {
      workingDirectory = new File(config.getWorkingDirectory());
    }

    String scriptFilename = "harness" + this.config.getExecutionId() + ".sh";
    File scriptFile = new File(workingDirectory, scriptFilename);

    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
      outputStream.write(command.getBytes());

      String[] commandList = new String[] {"/bin/bash", scriptFilename};
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(workingDirectory)
                                            .environment(config.getEnvironment())
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), INFO);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), ERROR);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      saveExecutionLog(
          format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO, commandExecutionStatus);
    } catch (Exception e) {
      saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
    } finally {
      if (isEmpty(config.getWorkingDirectory())) {
        deleteFolderAndItsContent(Paths.get(workingDirectory.getAbsolutePath()));
      } else {
        Files.deleteIfExists(Paths.get(scriptFile.getAbsolutePath()));
      }
    }

    return commandExecutionStatus;
  }

  private static void deleteFolderAndItsContent(final Path folder) throws IOException {
    Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private CommandExecutionStatus executePowerShellScript(String command) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    File workingDirectory = isEmpty(config.getWorkingDirectory()) ? new File(defaultParentWorkingDirectory)
                                                                  : new File(config.getWorkingDirectory());

    String[] commandList = new String[] {"Powershell", "-c", psWrappedCommand(command)};
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(workingDirectory)
                                            .environment(config.getEnvironment())
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), INFO);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                saveExecutionLog(line.trim(), ERROR);
                                              }
                                            });

      ProcessResult processResult = processExecutor.execute();
      commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      saveExecutionLog(
          format("Command completed with ExitCode (%d)", processResult.getExitValue()), INFO, commandExecutionStatus);
    } catch (Exception e) {
      saveExecutionLog(format("Exception: %s", e), ERROR, commandExecutionStatus);
    }

    return commandExecutionStatus;
  }

  private String psWrappedCommand(String command) {
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = String.format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); Invoke-Expression $decoded",
        base64Command);
    return String.format("Invoke-Command -command {%s}", wrappedCommand);
  }

  private void saveExecutionLog(String line, LogLevel level) {
    saveExecutionLog(line, level, RUNNING);
  }

  private void saveExecutionLog(String line, LogLevel level, CommandExecutionStatus commandExecutionStatus) {
    logService.save(config.getAccountId(),
        aLog()
            .withAppId(config.getAppId())
            .withActivityId(config.getExecutionId())
            .withLogLevel(level)
            .withCommandUnitName(config.getCommandUnitName())
            .withHostName("localhost")
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }
}