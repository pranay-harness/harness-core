/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pcf.cfcli;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pcf.cfcli.command.LoginCliCommand;
import io.harness.pcf.cfcli.command.LoginCliCommand.LoginOptions;
import io.harness.pcf.cfcli.command.VersionCliCommand;
import io.harness.pcf.cfcli.command.VersionCliCommand.VersionOptions;
import io.harness.pcf.model.CfCliVersion;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class CfCliCommandBuilderTest extends CategoryTest {
  public static final String PATH_TO_CF_CLI6 = "/path-to-cli6/cf";
  public static final String PATH_TO_CF_CLI7 = "/path-to-cli7/cf7";
  public static final String CF_CLI_DEFAULT_PATH = "cf";
  public static final String API_ENDPOINT_OPTION = "apiEndpoint";
  public static final String ORG_OPTION = "org";
  public static final String USER_OPTION = "user";
  public static final String PWD_OPTION = "pwd";
  public static final String SPACE_OPTION = "space";
  public static final String SSO_PASSCODE_OPTION = "ssoPasscode";
  public static final String ORIGIN_OPTION = "origin";

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliVersionCommandPath() {
    String cliVersionCommand = VersionCliCommand.builder()
                                   .cliVersion(CfCliVersion.V6)
                                   .cliPath(CF_CLI_DEFAULT_PATH)
                                   .options(VersionOptions.builder().version(true).build())
                                   .build()
                                   .getCommand();

    assertThat(cliVersionCommand).isEqualTo("cf --version");

    cliVersionCommand = VersionCliCommand.builder()
                            .cliVersion(CfCliVersion.V6)
                            .cliPath(PATH_TO_CF_CLI6)
                            .options(VersionOptions.builder().version(true).build())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand).isEqualTo("/path-to-cli6/cf --version");

    cliVersionCommand = VersionCliCommand.builder()
                            .cliVersion(CfCliVersion.V7)
                            .cliPath(CF_CLI_DEFAULT_PATH)
                            .options(VersionOptions.builder().version(true).build())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand).isEqualTo("cf --version");

    cliVersionCommand = VersionCliCommand.builder()
                            .cliVersion(CfCliVersion.V7)
                            .cliPath(PATH_TO_CF_CLI7)
                            .options(VersionOptions.builder().version(true).build())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand).isEqualTo("/path-to-cli7/cf7 --version");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliLoginCommandPathWithAllOptionsAndFlags() {
    String cliVersionCommand = LoginCliCommand.builder()
                                   .cliPath(CF_CLI_DEFAULT_PATH)
                                   .cliVersion(CfCliVersion.V6)
                                   .options(buildLoginOptionsWithAllOptionsAndFlags())
                                   .build()
                                   .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI6)
                            .cliVersion(CfCliVersion.V6)
                            .options(buildLoginOptionsWithAllOptionsAndFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(CF_CLI_DEFAULT_PATH)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptionsWithAllOptionsAndFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI7)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptionsWithAllOptionsAndFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli7/cf7 login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");
  }

  private LoginOptions buildLoginOptionsWithAllOptionsAndFlags() {
    return LoginOptions.builder()
        .apiEndpoint(API_ENDPOINT_OPTION)
        .org(ORG_OPTION)
        .user(USER_OPTION)
        .pwd(PWD_OPTION)
        .space(SPACE_OPTION)
        .ssoPasscode(SSO_PASSCODE_OPTION)
        .origin(ORIGIN_OPTION)
        .skipSslValidation(true)
        .sso(true)
        .build();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testValidateCommandArguments() {
    assertThatThrownBy(()
                           -> LoginCliCommand.builder()
                                  .cliPath(null)
                                  .cliVersion(CfCliVersion.V6)
                                  .options(buildLoginOptions())
                                  .build()
                                  .getCommand())
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Parameter cliPath cannot be null");

    assertThatThrownBy(()
                           -> LoginCliCommand.builder()
                                  .cliPath(CF_CLI_DEFAULT_PATH)
                                  .cliVersion(null)
                                  .options(buildLoginOptions())
                                  .build()
                                  .getCommand())
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Parameter cliVersion cannot be null");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliLoginCommandPath() {
    String cliVersionCommand = LoginCliCommand.builder()
                                   .cliPath(CF_CLI_DEFAULT_PATH)
                                   .cliVersion(CfCliVersion.V6)
                                   .options(buildLoginOptions())
                                   .build()
                                   .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI6)
                            .cliVersion(CfCliVersion.V6)
                            .options(buildLoginOptions())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(CF_CLI_DEFAULT_PATH)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptions())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI7)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptions())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli7/cf7 login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");
  }

  private LoginOptions buildLoginOptions() {
    return LoginOptions.builder()
        .apiEndpoint(API_ENDPOINT_OPTION)
        .sso(true)
        .org(ORG_OPTION)
        .user(USER_OPTION)
        .pwd(PWD_OPTION)
        .space(SPACE_OPTION)
        .ssoPasscode(SSO_PASSCODE_OPTION)
        .build();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliLoginCommandPathWithoutFlags() {
    String cliVersionCommand = LoginCliCommand.builder()
                                   .cliPath(CF_CLI_DEFAULT_PATH)
                                   .cliVersion(CfCliVersion.V6)
                                   .options(buildLoginOptionsWithoutFlags())
                                   .build()
                                   .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI6)
                            .cliVersion(CfCliVersion.V6)
                            .options(buildLoginOptionsWithoutFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(CF_CLI_DEFAULT_PATH)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptionsWithoutFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = LoginCliCommand.builder()
                            .cliPath(PATH_TO_CF_CLI7)
                            .cliVersion(CfCliVersion.V7)
                            .options(buildLoginOptionsWithoutFlags())
                            .build()
                            .getCommand();

    assertThat(cliVersionCommand)
        .isEqualTo("/path-to-cli7/cf7 login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");
  }

  private LoginOptions buildLoginOptionsWithoutFlags() {
    return LoginOptions.builder()
        .apiEndpoint(API_ENDPOINT_OPTION)
        .org(ORG_OPTION)
        .user(USER_OPTION)
        .pwd(PWD_OPTION)
        .space(SPACE_OPTION)
        .ssoPasscode(SSO_PASSCODE_OPTION)
        .build();
  }
}
