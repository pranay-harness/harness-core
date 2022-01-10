/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.RAGHVENDRA;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.workflow.StepYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(HarnessTeam.CDP)
public class CloudFormationProvisionStepYamlBuilderTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private AppService appService;
  @Mock private SecretManager secretManager;

  @InjectMocks private CloudFormationProvisionStepYamlBuilder cloudFormationProvisionStepYamlBuilder;

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithStackStatusesAndSkipConditionAsTrue() {
    List<String> stackStatusesToMarkAsSuccess = Arrays.asList("CREATE_COMPLETE", "UPDATE_ROLLBACK_COMPLETE");
    Map<String, Object> changeContextParams = getChangeContextParams(true, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatCode(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithNullStackStatusesAndSkipConditionAsTrue() {
    List<String> stackStatusesToMarkAsSuccess = null;
    Map<String, Object> changeContextParams = getChangeContextParams(true, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .hasMessage(
            "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is true, but the list stackStatusesToMarkAsSuccess is empty");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithEmptyStackStatusesAndSkipConditionAsTrue() {
    List<String> stackStatusesToMarkAsSuccess = new ArrayList<>();
    Map<String, Object> changeContextParams = getChangeContextParams(true, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .hasMessage(
            "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is true, but the list stackStatusesToMarkAsSuccess is empty");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithNullStackStatusesAndSkipConditionAsFalse() {
    List<String> stackStatusesToMarkAsSuccess = null;
    Map<String, Object> changeContextParams = getChangeContextParams(false, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatCode(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithEmptyStackStatusesAndSkipConditionAsFalse() {
    List<String> stackStatusesToMarkAsSuccess = null;
    Map<String, Object> changeContextParams = getChangeContextParams(false, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatCode(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithStackStatusesAndSkipConditionAsFalse() {
    List<String> stackStatusesToMarkAsSuccess = Arrays.asList("CREATE_COMPLETE", "UPDATE_ROLLBACK_COMPLETE");
    Map<String, Object> changeContextParams = getChangeContextParams(false, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext))
        .hasMessage(
            "Provided CloudFormation Step Yaml is Invalid: skipBasedOnStackStatus is false, but the list stackStatusesToMarkAsSuccess is not empty");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testValidateWithNullStackStatusesAndSkipConditionAsNull() {
    List<String> stackStatusesToMarkAsSuccess = null;
    Map<String, Object> changeContextParams = getChangeContextParams(null, stackStatusesToMarkAsSuccess);
    ChangeContext changeContext = buildChangeContext(changeContextParams);
    assertThatCode(() -> cloudFormationProvisionStepYamlBuilder.validate(changeContext)).doesNotThrowAnyException();
  }

  private ChangeContext buildChangeContext(Map<String, Object> parameters) {
    return ChangeContext.Builder.aChangeContext()
        .withYaml(StepYaml.builder().properties(parameters).build())
        .withChange(Change.Builder.aFileChange().withAccountId(WingsTestConstants.ACCOUNT_ID).build())
        .build();
  }

  private Map<String, Object> getChangeContextParams(
      Boolean skipBasedOnStackStatus, List<String> stackStatusesToMarkAsSuccess) {
    Map<String, Object> changeContextParams = new HashMap<>();
    changeContextParams.put("stackStatusesToMarkAsSuccess", stackStatusesToMarkAsSuccess);
    changeContextParams.put("skipBasedOnStackStatus", skipBasedOnStackStatus);
    return changeContextParams;
  }
}
