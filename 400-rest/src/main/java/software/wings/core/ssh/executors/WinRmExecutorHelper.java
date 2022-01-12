/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.core.ssh.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.core.winrm.executors.WinRmSession;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class WinRmExecutorHelper {
  private static final int SPLITLISTOFCOMMANDSBY = 20;

  /**
   * To construct the powershell script for running on target windows host.
   * @param command  Command String
   * @return Parsed command after escaping special characters. Command will write a powershell script file and then
   * execute it. Due to character limit for single powershell command, the command is split at a new line character and
   * writes one line at a time.
   */
  public static List<List<String>> constructPSScriptWithCommands(
      String command, String psScriptFile, String powershell) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;

    // Yes, replace() is intentional. We are replacing only character and not a regex pattern.
    command = command.replace("$", "`$");
    // This is to escape quotes
    command = command.replaceAll("\"", "`\\\\\"");

    // write commands to a file and then execute the file
    String appendPSInvokeCommandtoCommandString;
    appendPSInvokeCommandtoCommandString =
        powershell + " Invoke-Command -command {[IO.File]::AppendAllText(\\\"%s\\\", \\\"%s\\\" ) }";
    // Split the command at newline character
    List<String> listofCommands = Arrays.asList(command.split("\n"));

    // Replace pipe only if part of a string, else skip
    Pattern patternForPipeWithinAString = Pattern.compile("[a-zA-Z]+\\|");
    // Replace ampersand only if part of a string, else skip
    Pattern patternForAmpersandWithinString = Pattern.compile("[a-zA-Z0-9]+&");
    List<String> commandList = new ArrayList<>();
    for (String commandString : listofCommands) {
      if (patternForPipeWithinAString.matcher(commandString).find()) {
        commandString = commandString.replaceAll("\\|", "`\\\"|`\\\"");
      }
      if (patternForAmpersandWithinString.matcher(commandString).find()) {
        commandString = commandString.replaceAll("&", "^&");
      }
      // Append each command with PS Invoke command which is write command to file and also add the PS newline character
      // for correct escaping
      commandList.add(format(appendPSInvokeCommandtoCommandString, psScriptFile, commandString + "`r`n"));
    }
    return Lists.partition(commandList, SPLITLISTOFCOMMANDSBY);
  }

  public static String getScriptExecutingCommand(String psScriptFile, String powershell) {
    return format("%s -f \"%s\" ", powershell, psScriptFile);
  }

  public static List<String> constructPSScriptWithCommandsBulk(String command, String psScriptFile, String powershell) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;

    // Yes, replace() is intentional. We are replacing only character and not a regex pattern.
    command = command.replace("$", "`$");
    // This is to escape quotes
    command = command.replaceAll("\"", "`\\\\\"");

    // This is to change replace by new line char that powershell understands
    command = command.replaceAll("\n", "`n");

    // write commands to a file and then execute the file
    String appendPSInvokeCommandtoCommandString;
    appendPSInvokeCommandtoCommandString =
        powershell + " Invoke-Command -command {[IO.File]::WriteAllText(\\\"%s\\\", \\\"%s\\\" ) }";

    // Replace pipe only if part of a string, else skip
    Pattern patternForPipeWithinAString = Pattern.compile("[a-zA-Z]+\\|");
    // Replace ampersand only if part of a string, else skip
    Pattern patternForAmpersandWithinString = Pattern.compile("[a-zA-Z0-9]+&");
    if (patternForPipeWithinAString.matcher(command).find()) {
      command = command.replaceAll("\\|", "`\\\"|`\\\"");
    }
    if (patternForAmpersandWithinString.matcher(command).find()) {
      command = command.replaceAll("&", "^&");
    }
    // Append each command with PS Invoke command which is write command to file and also add the PS newline character
    // for correct escaping
    List<String> commandList = new ArrayList<>();
    commandList.add(format(appendPSInvokeCommandtoCommandString, psScriptFile, command + "`r`n"));
    // last command to run the script we just built - This will execute our command.
    commandList.add(format("%s -f \"%s\" ", powershell, psScriptFile));
    return commandList;
  }

  /**
   * Constructs powershell command by encoding the command string to base64 command.
   * @param command Command String
   * @param powershell
   * @return powershell command string that will convert that command from base64 to UTF8 string on windows host and
   * then run it on cmd.
   */
  @VisibleForTesting
  public static String psWrappedCommandWithEncoding(String command, String powershell) {
    command = "$ErrorActionPreference=\"Stop\"\n" + command;
    String base64Command = encodeBase64String(command.getBytes(StandardCharsets.UTF_8));
    String wrappedCommand = format(
        "$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\\\"%s\\\")); $expanded = [Environment]::ExpandEnvironmentVariables($decoded); Invoke-Expression $expanded",
        base64Command);
    return format("%s Invoke-Command -command {%s}", powershell, wrappedCommand);
  }

  @VisibleForTesting
  public static void cleanupFiles(
      WinRmSession session, String file, String powershell, boolean disableCommandEncoding) {
    if (file == null) {
      return;
    }

    String command = "Remove-Item -Path '" + file + "'";
    try (StringWriter outputAccumulator = new StringWriter(1024)) {
      if (disableCommandEncoding) {
        command = format(
            "%s Invoke-Command -command {$FILE_PATH=[System.Environment]::ExpandEnvironmentVariables(\\\"%s\\\") ;  Remove-Item -Path $FILE_PATH}",
            powershell, file);
        session.executeCommandString(command, outputAccumulator, outputAccumulator, false);
      } else {
        session.executeCommandString(
            psWrappedCommandWithEncoding(command, powershell), outputAccumulator, outputAccumulator, false);
      }
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      log.error("Exception while trying to remove file {} {}", file, e);
    }
  }
}
