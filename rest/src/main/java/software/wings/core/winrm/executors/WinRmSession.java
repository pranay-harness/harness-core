package software.wings.core.winrm.executors;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;

import java.io.StringWriter;

public class WinRmSession implements AutoCloseable {
  private static final int operationTimeout = 30 * 60 * 1000;
  private static final int retryCount = 1;

  private final ShellCommand shell;

  public WinRmSession(WinRmSessionConfig config) {
    WinRmClient client = WinRmClient.builder(getEndpoint(config.getHostname(), config.getPort(), config.isUseSSL()))
                             .disableCertificateChecks(config.isSkipCertChecks())
                             .authenticationScheme(getAuthSchemeString(config.getAuthenticationScheme()))
                             .credentials(config.getDomain(), config.getUsername(), config.getPassword())
                             .workingDirectory(config.getWorkingDirectory())
                             .environment(config.getEnvironment())
                             .retriesForConnectionFailures(retryCount)
                             .operationTimeout(operationTimeout)
                             .build();

    shell = client.createShell();
  }

  public int executeCommandString(String command, StringWriter output, StringWriter error) {
    try {
      return shell.execute(command, output, error);
    } catch (Exception e) {
      return 1;
    }
  }

  private static String getEndpoint(String hostname, int port, boolean useHttps) {
    return String.format("%s://%s:%d/wsman", useHttps ? "https" : "http", hostname, port);
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

  public void close() {
    if (shell != null) {
      shell.close();
    }
  }
}
