package software.wings.helpers.ext.shell.response;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse.ShellExecutionResponseBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Singleton
public class ShellExecutionServiceImpl implements ShellExecutionService {
  private static final Logger logger = LoggerFactory.getLogger(ShellExecutionServiceImpl.class);
  private static final String defaultParentWorkingDirectory = "./local-scripts/";
  private static final String ARTIFACT_RESULT_PATH = "ARTIFACT_RESULT_PATH";

  @Override
  public ShellExecutionResponse execute(ShellExecutionRequest shellExecutionRequest) {
    ShellExecutionResponseBuilder shellExecutionResponseBuilder = ShellExecutionResponse.builder();
    File workingDirectory;

    UUID uuid = UUID.randomUUID();
    if (isEmpty(shellExecutionRequest.getWorkingDirectory())) {
      String directoryPath = defaultParentWorkingDirectory + uuid.toString();
      try {
        createDirectoryIfDoesNotExist(directoryPath);
      } catch (IOException e) {
        logger.error("Exception in creating directory", e);
      }
      workingDirectory = new File(directoryPath);
    } else {
      workingDirectory = new File(shellExecutionRequest.getWorkingDirectory());
    }
    String scriptFilename = "harness-" + uuid.toString() + ".sh";
    File scriptFile = new File(workingDirectory, scriptFilename);

    String scriptOutputFilename = "harness-" + uuid.toString() + ".out";
    File scriptOutputFile = new File(workingDirectory, scriptOutputFilename);
    String command = shellExecutionRequest.getScriptString();
    command = addEnvVariablesCollector(command, scriptOutputFile.getAbsolutePath());

    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
      outputStream.write(command.getBytes(Charset.forName("UTF-8")));
      String[] commandList = new String[] {"/bin/bash", scriptFilename};
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(shellExecutionRequest.getTimeoutMillis(), MILLISECONDS)
                                            .command(commandList)
                                            .directory(workingDirectory)
                                            .readOutput(true);
      ProcessResult processResult = processExecutor.execute();
      shellExecutionResponseBuilder.exitValue(processResult.getExitValue());
      if (processResult.getExitValue() == 0) {
        Map<String, String> scriptData = new HashMap<>();
        scriptData.put(ARTIFACT_RESULT_PATH, scriptOutputFile.getAbsolutePath());
        shellExecutionResponseBuilder.shellExecutionData(scriptData);
      }
    } catch (IOException | InterruptedException | TimeoutException e) {
      logger.error("Exception in Script execution ", e);
      shellExecutionResponseBuilder.exitValue(1);
    } finally {
      try {
        deleteFileIfExists(scriptFile.getAbsolutePath());
      } catch (IOException e) {
        logger.info("Failed to delete file: ", scriptFile.getAbsolutePath());
      }
    }

    return shellExecutionResponseBuilder.build();
  }

  private String addEnvVariablesCollector(String command, String scriptOutputFilePath) {
    StringBuilder wrapperCommand = new StringBuilder();
    wrapperCommand.append("export " + ARTIFACT_RESULT_PATH + "=\"" + scriptOutputFilePath + "\"\n" + command);
    return wrapperCommand.toString();
  }
}
