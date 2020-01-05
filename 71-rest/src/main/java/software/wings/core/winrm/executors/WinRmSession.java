package software.wings.core.winrm.executors;

import static io.harness.windows.CmdUtils.escapeEnvValueSpecialChars;
import static java.lang.String.format;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WinRmSession implements AutoCloseable {
  private static final int operationTimeout = 30 * 60 * 1000;
  private static final int retryCount = 1;

  private final ShellCommand shell;

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
                             .operationTimeout(operationTimeout)
                             .build();

    shell = client.createShell();
  }

  public int executeCommandString(String command, Writer output, Writer error) {
    return shell.execute(command, output, error);
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
