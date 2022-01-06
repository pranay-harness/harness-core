/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.Flag;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Option;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
@Alias(value = "l")
public class LoginCliCommand extends CfCliCommand {
  @Builder
  LoginCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      LoginOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.LOGIN, arguments, options);
  }

  @Value
  @Builder
  public static class LoginOptions implements Options {
    @Option(value = "-a") String apiEndpoint;
    @Option(value = "-u") String user;
    @Option(value = "-p") String pwd;
    @Option(value = "-o") String org;
    @Option(value = "-s") String space;
    @Flag(value = "--sso") boolean sso;
    @Option(value = "--sso-passcode") String ssoPasscode;
    @Option(value = "--origin") String origin;
    @Flag(value = "--skip-ssl-validation") boolean skipSslValidation;
  }
}
