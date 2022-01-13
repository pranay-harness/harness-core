/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;

import io.harness.beans.SecretText;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.yaml.BaseYaml;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.yaml.handler.YamlHandlerTestBase;
import software.wings.yaml.handler.connectors.configyamlhandlers.SettingValueYamlConfig;

import com.google.inject.Inject;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public abstract class SettingValueConfigYamlHandlerTestBase extends YamlHandlerTestBase {
  @InjectMocks @Inject protected SecretManager secretManager;
  @InjectMocks @Inject protected YamlHelper yamlHelper;
  @InjectMocks @Inject protected SettingsService settingsService;
  @Mock protected SettingValidationService settingValidationService;

  public static final String verificationProviderYamlDir = "Setup/Verification Providers/";
  public static final String cloudProviderYamlDir = "Setup/Cloud Providers/";
  public static final String artifactServerYamlDir = "Setup/Artifact Servers/";
  public static final String loadBalancerYamlDir = "Setup/Load Balancers/";
  public static final String collaborationProviderYamlDir = "Setup/Collaboration Providers/";
  public static final String invalidYamlPath = "Setup/Verification Providers Invalid/invalid.yaml";
  public static final String accountName = "accountName";
  public static final String userName = "username";
  public static final String password = "password";
  public static final String token = "token";
  public static final String apiKey = "key";
  public static final String acessId = "accessId";
  public static final String accesskey = "accesskey";

  protected String getYamlFilePath(String name, String path) {
    return new StringBuilder().append(path).append(name).append(".yaml").toString();
  }

  protected <T extends BaseYaml> ChangeContext<T> getChangeContext(
      String yamlContent, String yamlPath, Class yamlClass, BaseYamlHandler yamlHandler) throws IOException {
    T yamlObject = (T) getYaml(yamlContent, yamlClass);

    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withFileContent(yamlContent)
                                      .withFilePath(yamlPath)
                                      .withAccountId(ACCOUNT_ID)
                                      .build();

    ChangeContext<T> changeContext = ChangeContext.Builder.aChangeContext()
                                         .withChange(gitFileChange)
                                         .withYamlType(YamlType.SETTING_VALUE)
                                         .withYamlSyncHandler(yamlHandler)
                                         .build();

    changeContext.setYaml(yamlObject);
    return changeContext;
  }

  protected void testCRUD(SettingValueYamlConfig settingValueYamlConfig) throws Exception {
    SettingAttribute settingAttributeSaved = settingValueYamlConfig.getSettingAttributeSaved();
    SettingValueYamlHandler yamlHandler = settingValueYamlConfig.getYamlHandler();
    String yamlFilePath = getYamlFilePath(settingValueYamlConfig.getName(), settingValueYamlConfig.getYamlDirPath());

    SettingAttribute fetchedSettingAttribute =
        (SettingAttribute) settingValueYamlConfig.getYamlHandler().get(ACCOUNT_ID, yamlFilePath);
    assertThat(fetchedSettingAttribute).isNotNull();
    verify(settingValueYamlConfig.getSettingAttributeSaved(), fetchedSettingAttribute);

    // 2. update and get Yaml String from SettingAttribute
    if (!StringUtils.isEmpty(settingValueYamlConfig.getCurrentFieldValue())) {
      updateSettingAttribute(settingValueYamlConfig);
    }
    String yamlContent = getYamlContentString(settingAttributeSaved, settingValueYamlConfig.getYamlHandler());

    // 3. call upsert() using this yamlContent
    ChangeContext changeContext =
        getChangeContext(yamlContent, yamlFilePath, settingValueYamlConfig.getYamlClass(), yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete  record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = (SettingAttribute) yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertThat(settingAttributeDeleted).isNull();
  }

  protected void testFailureScenario(SettingValueYamlConfig settingValueYamlConfig)
      throws IOException, HarnessException {
    SettingValueYamlHandler yamlHandler = settingValueYamlConfig.getYamlHandler();
    String yamlFilePath = getYamlFilePath(settingValueYamlConfig.getName(), settingValueYamlConfig.getYamlDirPath());

    SettingAttribute settingAttribute = (SettingAttribute) yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertThat(settingAttribute).isNotNull();

    // 3. Now, Use invalid yaml path and make sure it upsertFromYaml fails
    String yamlContent = getYamlContentString(settingValueYamlConfig.getSettingAttributeSaved(), yamlHandler);
    ChangeContext changeContext =
        getChangeContext(yamlContent, invalidYamlPath, settingValueYamlConfig.getYamlClass(), yamlHandler);
    try {
      yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException ex) {
      // Do nothing
    }

    // 4. Now, Use invalid yaml content (missing encrypted password) and  make sure upsertFromYaml fails

    changeContext = getChangeContext(settingValueYamlConfig.getInvalidYamlContent(), yamlFilePath,
        settingValueYamlConfig.getYamlClass(), yamlHandler);
    thrown.expect(Exception.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }

  protected String getYamlContentString(SettingAttribute settingAttribute, BaseYamlHandler yamlHandler) {
    BaseYaml yaml = yamlHandler.toYaml(settingAttribute, settingAttribute.getAppId());

    if (yaml != null) {
      String yamlContent = getYamlContent(yaml);
      if (yamlContent != null) {
        return yamlContent.substring(0, yamlContent.length() - 1);
      }
    }

    return null;
  }

  protected void verify(SettingAttribute settingAttributeSaved, SettingAttribute settingAttribute) {
    assertThat(settingAttribute.getName()).isEqualTo(settingAttributeSaved.getName());
    assertThat(settingAttributeSaved.getValue()).isNotNull();
    assertThat(settingAttribute.getValue()).isNotNull();
    if (settingAttribute.getValue() instanceof EncryptableSetting) {
      ((EncryptableSetting) settingAttribute.getValue())
          .setDecrypted(((EncryptableSetting) settingAttributeSaved.getValue()).isDecrypted());
    }
    assertThat(settingAttribute.getValue().toString()).isEqualTo(settingAttributeSaved.getValue().toString());
  }

  private void updateSettingAttribute(SettingValueYamlConfig settingValueYamlConfig) {
    java.lang.reflect.Method method = null;
    try {
      method =
          settingValueYamlConfig.getConfigclazz().getMethod(settingValueYamlConfig.getUpdateMethodName(), String.class);
    } catch (Exception e) {
      assertThat(false).isTrue();
    }

    try {
      method.invoke(settingValueYamlConfig.getSettingAttributeSaved().getValue(),
          settingValueYamlConfig.getCurrentFieldValue() + "_1");
    } catch (Exception e) {
      // Do nothing
    }
  }

  protected String createSecretText(String accountId, String secretName, String secretValue) {
    return secretManager.saveSecretText(
        accountId, SecretText.builder().name(secretName).value(secretValue).build(), false);
  }
}
