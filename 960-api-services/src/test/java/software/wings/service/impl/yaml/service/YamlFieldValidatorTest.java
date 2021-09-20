package software.wings.service.impl.yaml.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.BOGDAN;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class YamlFieldValidatorTest {
  private YamlFieldValidator validator;

  @Before
  public void setUp() {
    validator = new YamlFieldValidator();
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void environmentWithNameAndValueShouldPassValidation() {
    // given
    String nameAndValueAddedYaml = "harnessApiVersion: '1.0'\n"
        + "type: ENVIRONMENT\n"
        + "configMapYamlByServiceTemplateName: {}\n"
        + "environmentType: NON_PROD\n"
        + "variableOverrides:\n"
        + "- name: aName\n"
        + "  value: aValue\n"
        + "  valueType: TEXT";

    // when
    validator.validateYaml(nameAndValueAddedYaml);

    // then no exception
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void environmentWithNoOverridesShouldPassValidation() {
    // given
    String noOverridesYaml = "harnessApiVersion: '1.0'\n"
        + "type: ENVIRONMENT\n"
        + "configMapYamlByServiceTemplateName: {}\n"
        + "environmentType: NON_PROD";

    // when
    validator.validateYaml(noOverridesYaml);

    // then no exception
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void environmentWithEmptyValueShouldValidate() {
    // given
    String emptyValueYaml = "harnessApiVersion: '1.0'\n"
        + "type: ENVIRONMENT\n"
        + "configMapYamlByServiceTemplateName: {}\n"
        + "environmentType: NON_PROD\n"
        + "variableOverrides:\n"
        + "- name: aName\n"
        + "  value: \n"
        + "  valueType: TEXT";

    // then
    assertThatThrownBy(() -> validator.validateYaml(emptyValueYaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Variable override value cannot be null.");
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void environmentWithEmptyNameShouldFailValidation() {
    // given
    String noVariableNameYaml = "harnessApiVersion: '1.0'\n"
        + "type: ENVIRONMENT\n"
        + "configMapYamlByServiceTemplateName: {}\n"
        + "environmentType: NON_PROD\n"
        + "variableOverrides:\n"
        + "- name: \n"
        + "  value: aValue\n"
        + "  valueType: TEXT";

    // then
    assertThatThrownBy(() -> validator.validateYaml(noVariableNameYaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Variable override name cannot be null.");
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void phasesDotsInNameShouldNotValidate() {
    // given
    String noNameAmiTag = "harnessApiVersion: '1.0'\n"
        + "type: ENVIRONMENT\n"
        + "configMapYamlByServiceTemplateName: {}\n"
        + "environmentType: NON_PROD\n"
        + "phases:\n"
        + "- name: a.name.with.dots\n";

    // then
    assertThatThrownBy(() -> validator.validateYaml(noNameAmiTag))
        .isInstanceOf(InvalidYamlNameException.class)
        .hasMessageContaining("Invalid phase name [a.name.with.dots]. Dots are not permitted");
  }
}
