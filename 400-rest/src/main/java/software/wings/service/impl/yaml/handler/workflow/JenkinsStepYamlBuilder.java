/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(CDC)
@Slf4j
public class JenkinsStepYamlBuilder extends StepYamlBuilder {
  private static final String JENKINS_ID = "jenkinsConfigId";
  private static final String JENKINS_NAME = "jenkinsConfigName";

  @Inject SettingsService settingsService;

  @Override
  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (JENKINS_ID.equals(name)) {
      if (objectValue != null) {
        String jenkinsConfigId = (String) objectValue;
        SettingAttribute jenkinsConfig = settingsService.get(jenkinsConfigId);
        notNullCheck("Jenkins is null for the given jenkinsConfigId:" + jenkinsConfigId, jenkinsConfig, USER);
        outputProperties.put(JENKINS_NAME, jenkinsConfig.getName());
      } else {
        outputProperties.put(JENKINS_NAME, null);
      }
      return;
    }
    outputProperties.put(name, objectValue);
  }

  @Override
  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    if (JENKINS_NAME.equals(name)) {
      if (objectValue != null) {
        String jenkinsConfigName = (String) objectValue;
        SettingAttribute jenkinsConfig = settingsService.getSettingAttributeByName(accountId, jenkinsConfigName);
        notNullCheck("Jenkins is null for the given jenkinsConfigName:" + jenkinsConfigName, jenkinsConfig, USER);
        outputProperties.put(JENKINS_ID, jenkinsConfig.getUuid());
      } else {
        outputProperties.put(JENKINS_ID, null);
      }
      return;
    }
    if (JENKINS_ID.equals(name)) {
      log.info(YAML_ID_LOG, "JENKINS", accountId);
    }
    outputProperties.put(name, objectValue);
  }
}
