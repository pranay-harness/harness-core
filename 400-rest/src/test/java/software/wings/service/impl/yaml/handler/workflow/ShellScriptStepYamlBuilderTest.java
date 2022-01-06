/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDC)
public class ShellScriptStepYamlBuilderTest extends StepYamlBuilderTestBase {
  @InjectMocks private ShellScriptStepYamlBuilder shellScriptStepYamlBuilder;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_Ssh() {
    Map<String, Object> inputProperties = getInputPropertiesForSshConnection(true, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> shellScriptStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForSshConnection(false, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_Ssh() {
    Map<String, Object> inputProperties = getInputPropertiesForSshConnection(false, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value)
            -> shellScriptStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForSshConnection(true, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_SshTemplatised() {
    Map<String, Object> inputProperties = getInputPropertiesForSshConnection(true, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> shellScriptStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForSshConnection(false, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_SshTemplatised() {
    Map<String, Object> inputProperties = getInputPropertiesForSshConnection(false, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value)
            -> shellScriptStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForSshConnection(true, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_Winrm() {
    Map<String, Object> inputProperties = getInputPropertiesForWinRmConnection(true, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> shellScriptStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForWinRmConnection(false, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_Winrm() {
    Map<String, Object> inputProperties = getInputPropertiesForWinRmConnection(false, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value)
            -> shellScriptStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForWinRmConnection(true, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_WinrmTemplatised() {
    Map<String, Object> inputProperties = getInputPropertiesForWinRmConnection(true, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> shellScriptStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForWinRmConnection(false, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_WinrmTemplatised() {
    Map<String, Object> inputProperties = getInputPropertiesForWinRmConnection(false, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value)
            -> shellScriptStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputPropertiesForWinRmConnection(true, true));
  }

  private Map<String, Object> getInputPropertiesForSshConnection(boolean withName, boolean isTemplatised) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(SSH_KEY_REF_NAME, isTemplatised ? null : SSH_KEY_REF_NAME);
    } else {
      inputProperties.put(SSH_KEY_REF, isTemplatised ? null : SSH_KEY_REF);
    }
    inputProperties.put("commandPath", "tmp/");
    inputProperties.put(withName ? CONNECTION_ATTRIBUTE_NAME : CONNECTION_ATTRIBUTES, null);
    inputProperties.put("connectionType", "SSH");
    inputProperties.put("executeOnDelegate", false);
    inputProperties.put("host", "localhost");
    inputProperties.put("scriptString", "echo Hi!");
    inputProperties.put("scriptType", "BASH");
    inputProperties.put("templateUuid", null);
    inputProperties.put("timeoutMillis", 123);
    if (isTemplatised) {
      inputProperties.put(TEMPLATE_EXPRESSIONS, getTemplateExpressions("${ssh_connection_Attribute}", SSH_KEY_REF));
    }
    return inputProperties;
  }

  private Map<String, Object> getInputPropertiesForWinRmConnection(boolean withName, boolean isTemplatised) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(CONNECTION_ATTRIBUTE_NAME, isTemplatised ? null : CONNECTION_ATTRIBUTE_NAME);
    } else {
      inputProperties.put(CONNECTION_ATTRIBUTES, isTemplatised ? null : CONNECTION_ATTRIBUTES);
    }
    inputProperties.put("commandPath", "C:");
    inputProperties.put(withName ? SSH_KEY_REF_NAME : SSH_KEY_REF, null);
    inputProperties.put("connectionType", "WINRM");
    inputProperties.put("executeOnDelegate", false);
    inputProperties.put("host", "localhost");
    inputProperties.put("scriptString", "echo Hi!");
    inputProperties.put("scriptType", "POWERSHELL");
    inputProperties.put("templateUuid", null);
    inputProperties.put("timeoutMillis", 123);
    if (isTemplatised) {
      inputProperties.put(
          TEMPLATE_EXPRESSIONS, getTemplateExpressions("${winrm_connection_Attribute}", CONNECTION_ATTRIBUTES));
    }
    return inputProperties;
  }
}
