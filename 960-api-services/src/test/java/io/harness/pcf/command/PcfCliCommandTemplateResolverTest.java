package io.harness.pcf.command;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pcf.command.option.LoginOptions;
import io.harness.pcf.model.PcfCliVersion;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PcfCliCommandTemplateResolverTest extends CategoryTest {
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
    String cliVersionCommand = PcfCliCommandTemplateResolver.getCliVersionCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V6).build());

    assertThat(cliVersionCommand).isEqualTo("cf --version");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliVersionCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI6).cliVersion(PcfCliVersion.V6).build());

    assertThat(cliVersionCommand).isEqualTo("/path-to-cli6/cf --version");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliVersionCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V7).build());

    assertThat(cliVersionCommand).isEqualTo("cf --version");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliVersionCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI7).cliVersion(PcfCliVersion.V7).build());

    assertThat(cliVersionCommand).isEqualTo("/path-to-cli7/cf7 --version");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliLoginCommandPathWithAllOptionsAndFlags() {
    String cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V6).build(),
        buildLoginOptionsWithAllOptionsAndFlags());

    assertThat(cliVersionCommand)
        .isEqualTo(
            "cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI6).cliVersion(PcfCliVersion.V6).build(),
        buildLoginOptionsWithAllOptionsAndFlags());

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V7).build(),
        buildLoginOptionsWithAllOptionsAndFlags());

    assertThat(cliVersionCommand)
        .isEqualTo(
            "cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode --origin origin --skip-ssl-validation");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI7).cliVersion(PcfCliVersion.V7).build(),
        buildLoginOptionsWithAllOptionsAndFlags());

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
    assertThatThrownBy(() -> PcfCliCommandTemplateResolver.getCliLoginCommand(null, LoginOptions.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Parameter commandArguments cannot be null");

    assertThatThrownBy(()
                           -> PcfCliCommandTemplateResolver.getCliLoginCommand(
                               CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(null).build(),
                               LoginOptions.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Parameter cliVersion cannot be null");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCliLoginCommandPath() {
    String cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V6).build(),
        buildLoginOptions());

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI6).cliVersion(PcfCliVersion.V6).build(), buildLoginOptions());

    assertThat(cliVersionCommand)
        .isEqualTo(
            "/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V7).build(),
        buildLoginOptions());

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI7).cliVersion(PcfCliVersion.V7).build(), buildLoginOptions());

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
    String cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V6).build(),
        buildLoginOptionsWithoutFlags());

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI6).cliVersion(PcfCliVersion.V6).build(),
        buildLoginOptionsWithoutFlags());

    assertThat(cliVersionCommand)
        .isEqualTo("/path-to-cli6/cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(CF_CLI_DEFAULT_PATH).cliVersion(PcfCliVersion.V7).build(),
        buildLoginOptionsWithoutFlags());

    assertThat(cliVersionCommand)
        .isEqualTo("cf login -a apiEndpoint -u user -p pwd -o org -s space --sso-passcode ssoPasscode");

    cliVersionCommand = PcfCliCommandTemplateResolver.getCliLoginCommand(
        CommandArguments.builder().cliPath(PATH_TO_CF_CLI7).cliVersion(PcfCliVersion.V7).build(),
        buildLoginOptionsWithoutFlags());

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
