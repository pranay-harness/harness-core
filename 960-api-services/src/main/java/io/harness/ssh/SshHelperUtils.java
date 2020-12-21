package io.harness.ssh;

import static io.harness.logging.LogLevel.ERROR;

import static java.lang.String.format;

import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.jcraft.jsch.JSchException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
public class SshHelperUtils {
  public static void generateTGT(String userPrincipal, String password, String keyTabFilePath, LogCallback logCallback)
      throws JSchException {
    if (!isValidKeyTabFile(keyTabFilePath)) {
      logCallback.saveExecutionLog("Cannot proceed with Ticket Granting Ticket(TGT) generation.", ERROR);
      log.error("Cannot proceed with Ticket Granting Ticket(TGT) generation");
      throw new JSchException(
          "Failure: Invalid keytab file path. Cannot proceed with Ticket Granting Ticket(TGT) generation");
    }
    log.info("Generating Ticket Granting Ticket(TGT)...");
    logCallback.saveExecutionLog("Generating Ticket Granting Ticket(TGT) for principal: " + userPrincipal);
    String commandString = !StringUtils.isEmpty(password) ? format("echo \"%s\" | kinit %s", password, userPrincipal)
                                                          : format("kinit -k -t %s %s", keyTabFilePath, userPrincipal);
    boolean ticketGenerated = executeLocalCommand(commandString, logCallback, null, false);
    if (ticketGenerated) {
      logCallback.saveExecutionLog("Ticket Granting Ticket(TGT) generated successfully for " + userPrincipal);
      log.info("Ticket Granting Ticket(TGT) generated successfully for " + userPrincipal);
    } else {
      log.error("Failure: could not generate Ticket Granting Ticket(TGT)");
      throw new JSchException("Failure: could not generate Ticket Granting Ticket(TGT)");
    }
  }

  private static boolean isValidKeyTabFile(String keyTabFilePath) {
    if (!StringUtils.isEmpty(keyTabFilePath)) {
      if (new File(keyTabFilePath).exists()) {
        log.info("Found keytab file at path: [{}]", keyTabFilePath);
        return true;
      } else {
        log.error("Invalid keytab file path: [{}].", keyTabFilePath);
        return false;
      }
    }
    return true;
  }

  public static boolean executeLocalCommand(
      String cmdString, LogCallback logCallback, Writer output, boolean isOutputWriter) {
    String[] commandList = new String[] {"/bin/bash", "-c", cmdString};
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ByteArrayOutputStream byteArrayErrorStream = new ByteArrayOutputStream()) {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(new File(System.getProperty("user.home")))
                                            .readOutput(true)
                                            .redirectOutput(byteArrayOutputStream)
                                            .redirectError(byteArrayErrorStream);

      ProcessResult processResult = null;
      try {
        processResult = processExecutor.execute();
      } catch (IOException | InterruptedException | TimeoutException e) {
        log.error("Failed to execute command ", e);
      }
      if (byteArrayOutputStream.toByteArray().length != 0) {
        if (isOutputWriter) {
          try {
            output.write(byteArrayOutputStream.toString());
          } catch (IOException e) {
            log.error("Failed to store the output to writer ", e);
          }
        } else {
          logCallback.saveExecutionLog(byteArrayOutputStream.toString(), LogLevel.INFO);
        }
      }
      if (byteArrayErrorStream.toByteArray().length != 0) {
        logCallback.saveExecutionLog(byteArrayErrorStream.toString(), ERROR);
      }
      return processResult != null && processResult.getExitValue() == 0;
    } catch (IOException e) {
      log.error("Failed to execute command ", e);
    }
    return false;
  }
}
