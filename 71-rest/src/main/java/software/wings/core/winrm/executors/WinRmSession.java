package software.wings.core.winrm.executors;

import static io.harness.windows.CmdUtils.escapeEnvValueSpecialChars;
import static java.lang.String.format;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WinRmSession implements AutoCloseable {
  private static final int retryCount = 1;

  private final ShellCommand shell;
  private final WinRmTool winRmTool;

  public WinRmSession(WinRmSessionConfig config) {
    Map<String, String> processedEnvironmentMap = new HashMap<>();
    if (config.getEnvironment() != null) {
      for (Entry<String, String> entry : config.getEnvironment().entrySet()) {
        processedEnvironmentMap.put(entry.getKey(), escapeEnvValueSpecialChars(entry.getValue()));
      }
    }

    WinRmClient client = WinRmClient.builder(getEndpoint(config.getHostname(), config.getPort(), config.isUseSSL()))
                             .disableCertificateChecks(config.isSkipCertChecks())
                             .authenticationScheme(getAuthSchemeString(config.getAuthenticationScheme()))
                             .credentials(config.getDomain(), config.getUsername(), config.getPassword())
                             .workingDirectory(config.getWorkingDirectory())
                             .environment(processedEnvironmentMap)
                             .retriesForConnectionFailures(retryCount)
                             .operationTimeout(config.getTimeout())
                             .build();

    WinRmClientContext context = WinRmClientContext.newInstance();

    winRmTool = WinRmTool.Builder.builder(config.getHostname(), config.getUsername(), config.getPassword())
                    .disableCertificateChecks(config.isSkipCertChecks())
                    .authenticationScheme(getAuthSchemeString(config.getAuthenticationScheme()))
                    .workingDirectory(config.getWorkingDirectory())
                    .environment(processedEnvironmentMap)
                    .port(config.getPort())
                    .useHttps(config.isUseSSL())
                    .context(context)
                    .build();

    shell = client.createShell();
  }

  public int executeCommandString(String command, Writer output, Writer error) {
    return shell.execute(command, output, error);
  }

  public WinRmToolResponse executeCommandsList(List<List<String>> commandList) {
    WinRmToolResponse winRmToolResponse = null;
    for (List<String> list : commandList) {
      winRmToolResponse = winRmTool.executeCommand(list);
      if (winRmToolResponse.getStatusCode() != 0) {
        break;
      }
    }
    return winRmToolResponse;
  }

  private static String getEndpoint(String hostname, int port, boolean useHttps) {
    return format("%s://%s:%d/wsman", useHttps ? "https" : "http", hostname, port);
  }

  private static String getAuthSchemeString(AuthenticationScheme authenticationScheme) {
    switch (authenticationScheme) {
      case BASIC:
        return "Basic";
      case NTLM:
        return "NTLM";
      default:
        return "Unknown";
    }
  }

  @Override
  public void close() {
    if (shell != null) {
      shell.close();
    }
  }
}
