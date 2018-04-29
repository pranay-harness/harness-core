package software.wings.integration;

import static java.lang.String.format;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import org.apache.http.client.config.AuthSchemes;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.Integration;

import java.io.StringWriter;

@Integration
@Ignore
/*
This test class is making calls to WinRM. The purpose is to do directed testing for WinRM APIs.
This is not to be run as an automated test[hence the @Ignore].
 */
public class WinRmToolIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(WinRmToolIntegrationTest.class);

  private static void setUp() {}

  public static void main(String[] args) {
    logger.info("WinRmToolIntegrationTest: Start.");
    // test2();
    logger.info(getEndpoint("localhost", 80, true));
    logger.info(psWrappedCommand("echo hello\r\ndir"));

    logger.info("WinRmToolIntegrationTest: Done.");
  }

  private static String localhost = "https://localhost:5986/wsman";
  private static String azureHost = "https://104.209.40.95:5986/wsman";

  private static String getEndpoint(String hostname, int port, boolean useHttps) {
    return format("%s://%s:%d/wsman", useHttps ? "https" : "http", hostname, port);
  }

  private static String psWrappedCommand(String command) {
    String normalizedCommand = command.replace("\r", "").replace("\n", ";");
    return format("Powershell Invoke-Command -command {%s}", normalizedCommand);
  }

  private static void test2() {
    WinRmClient client = WinRmClient
                             .builder(azureHost)
                             //.authenticationScheme("Basic")
                             .disableCertificateChecks(true)
                             .credentials("harnessadmin", "H@rnessH@rness")
                             .workingDirectory("c:")
                             .retriesForConnectionFailures(1)
                             .build();

    int exitCode = 999;
    try (ShellCommand shell = client.createShell()) {
      StringWriter out = new StringWriter();
      StringWriter err = new StringWriter();
      exitCode = shell.execute("PowerShell Get-ChildItem", out, err);
      logger.info(out.toString());
      logger.info(err.toString());
      // exitCode = shell.execute("dir /s", out, err);
      // logger.info(out.toString());
    }
  }

  private static void test1() {
    WinRmClientContext context = WinRmClientContext.newInstance();

    WinRmTool.Builder builder = WinRmTool.Builder.builder("104.209.40.95", "harnessadmin1", "H@rnessH@rness");
    builder.authenticationScheme(AuthSchemes.NTLM);
    builder.port(5986);
    builder.useHttps(true);
    builder.disableCertificateChecks(true);
    builder.context(context);
    WinRmTool tool = builder.build();

    try {
      tool.setRetriesForConnectionFailures(1);
      tool.executePs("echo hi");
    } catch (Exception e) {
      logger.error(e.getMessage());
    }

    context.shutdown();
  }
}