package io.harness.pcf.command.option;

import lombok.Builder;

@Builder
public class LoginOptions extends Options {
  @Option(value = "-a") private String apiEndpoint;

  @Option(value = "-u") private String user;

  @Option(value = "-p") private String pwd;

  @Option(value = "-o") private String org;

  @Option(value = "-s") private String space;

  @Flag(value = "--sso") private boolean sso;

  @Option(value = "--sso-passcode") private String ssoPasscode;

  @Option(value = "--origin") private String origin;

  @Flag(value = "--skip-ssl-validation") private boolean skipSslValidation;
}
