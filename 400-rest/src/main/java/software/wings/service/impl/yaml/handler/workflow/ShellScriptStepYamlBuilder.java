/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.workflow.StepYaml;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
public class ShellScriptStepYamlBuilder extends StepYamlBuilder {
  private static final String SSH_KEY_REF = "sshKeyRef";
  private static final String SSH_KEY_REF_NAME = "sshKeyRefName";
  private static final String CONNECTION_ATTRIBUTES = "connectionAttributes";
  private static final String CONNECTION_ATTRIBUTE_NAME = "connectionAttributeName";

  @Inject private SettingsService settingsService;

  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    String name = changeContext.getYaml().getName();
    if (name.contains(".")) {
      throw new InvalidRequestException("Shell script step [" + name + "] has '.' in its name");
    }
  }

  @Override
  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (SSH_KEY_REF.equals(name)) {
      if (EmptyPredicate.isNotEmpty((String) objectValue)) {
        SettingAttribute sshConnectionAttribute = settingsService.get((String) objectValue);
        notNullCheck("Ssh Key does not exist.", sshConnectionAttribute);
        outputProperties.put(SSH_KEY_REF_NAME, sshConnectionAttribute.getName());
      } else {
        outputProperties.put(SSH_KEY_REF_NAME, null);
      }
      return;
    }
    if (CONNECTION_ATTRIBUTES.equals(name)) {
      if (EmptyPredicate.isNotEmpty((String) objectValue)) {
        SettingAttribute winrmConnectionAttribute = settingsService.get((String) objectValue);
        notNullCheck("Winrm Connection Attribute does not exist.", winrmConnectionAttribute);
        outputProperties.put(CONNECTION_ATTRIBUTE_NAME, winrmConnectionAttribute.getName());
      } else {
        outputProperties.put(CONNECTION_ATTRIBUTE_NAME, null);
      }
      return;
    }
    outputProperties.put(name, objectValue);
  }

  @Override
  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    if (SSH_KEY_REF_NAME.equals(name)) {
      if (objectValue != null) {
        String sshConnectionAttributeName = (String) objectValue;
        SettingAttribute sshConnectionAttribute =
            settingsService.getSettingAttributeByName(accountId, sshConnectionAttributeName);
        notNullCheck(String.format("Ssh key %s does not exist.", sshConnectionAttributeName), sshConnectionAttribute);
        outputProperties.put(SSH_KEY_REF, sshConnectionAttribute.getUuid());
      } else {
        outputProperties.put(SSH_KEY_REF, null);
      }
      return;
    }
    if (CONNECTION_ATTRIBUTE_NAME.equals(name)) {
      if (objectValue != null) {
        String winrmConnectionAttributeName = (String) objectValue;
        SettingAttribute winrmConnectionAttribute =
            settingsService.getSettingAttributeByName(accountId, winrmConnectionAttributeName);
        notNullCheck(String.format("Winrm Connection Attribute %s does not exist.", winrmConnectionAttributeName),
            winrmConnectionAttribute);
        outputProperties.put(CONNECTION_ATTRIBUTES, winrmConnectionAttribute.getUuid());
      } else {
        outputProperties.put(CONNECTION_ATTRIBUTES, null);
      }
      return;
    }
    if (SSH_KEY_REF.equals(name)) {
      if (this instanceof CommandStepYamlBuilder) {
        log.info(YAML_ID_LOG, "COMMAND", accountId);
      } else {
        log.info(YAML_ID_LOG, "SHELL SCRIPT", accountId);
      }
    }
    if (CONNECTION_ATTRIBUTES.equals(name)) {
      if (this instanceof CommandStepYamlBuilder) {
        log.info(YAML_ID_LOG, "COMMAND", accountId);
      } else {
        log.info(YAML_ID_LOG, "SHELL SCRIPT", accountId);
      }
    }
    outputProperties.put(name, objectValue);
  }
}
