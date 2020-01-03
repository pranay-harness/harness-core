package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.JiraConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.JiraConfigYamlHandler;

import java.io.IOException;

public class JiraConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private JiraConfigYamlHandler yamlHandler;

  public static final String url = "jira.com";

  private String invalidYamlContent = "host_invalid: jira.com\n"
      + "username: support@harness.io\n"
      + "password: safeharness:DBAtpYCHSx2fPG8MIFQFmA\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: JIRA";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String name = "JIRA" + System.currentTimeMillis();

    // 1. Create JIRA record
    SettingAttribute settingAttributeSaved = createJIRAVerificationProvider(name);
    assertThat(settingAttributeSaved.getName()).isEqualTo(name);

    testCRUD(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  private SettingAttribute createJIRAVerificationProvider(String name) {
    // Generate JIRA connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.CONNECTOR)
                                    .withName(name)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(JiraConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .baseUrl(url)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(JiraConfig.Yaml.class)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(collaborationProviderYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(JiraConfig.class)
        .updateMethodName("setBaseUrl")
        .currentFieldValue(url)
        .build();
  }
}
