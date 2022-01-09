/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;

import software.wings.rules.Integration;

import io.cloudsoft.winrm4j.client.ShellCommand;
import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import java.io.StringWriter;
import lombok.extern.slf4j.Slf4j;

@Integration
/*
This test class is making calls to WinRM. The purpose is to do directed testing for WinRM APIs.
This is not to be run as an automated test[hence the @Ignore].
 */
@Slf4j
@OwnedBy(CDP)
public abstract class WinRmToolIntegrationTestBase extends CategoryTest {
  private static void setUp() {}

  public static void main(String[] args) {
    log.info("WinRmToolIntegrationTestBase: Start.");
    // test2();
    log.info(getEndpoint("localhost", 80, true));
    log.info(psWrappedCommand("echo hello\r\ndir"));

    log.info("WinRmToolIntegrationTestBase: Done.");
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
      log.info(out.toString());
      log.info(err.toString());
      // exitCode = shell.execute("dir /s", out, err);
      // log.info(out.toString());
    }
  }

  private static void test1() {
    WinRmClientContext context = WinRmClientContext.newInstance();

    WinRmTool.Builder builder = WinRmTool.Builder.builder("104.209.40.95", "harnessadmin1", "H@rnessH@rness");
    //    builder.authenticationScheme(AuthSchemes.NTLM);
    builder.port(5986);
    builder.useHttps(true);
    builder.disableCertificateChecks(true);
    builder.context(context);
    WinRmTool tool = builder.build();

    try {
      tool.setRetriesForConnectionFailures(1);
      tool.executePs("echo hi");
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    context.shutdown();
  }
}
