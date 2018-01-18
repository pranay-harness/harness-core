package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.SmtpConfigYamlHandler;

import java.io.IOException;

public class SmtpConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private SmtpConfigYamlHandler yamlHandler;

  public static final String url = "smtp.gmail.com";

  private String invalidYamlContent = "host_invalid: smtp.gmail.com\n"
      + "port: 465\n"
      + "fromAddress: support@harness.io\n"
      + "useSSL: true\n"
      + "username: support@harness.io\n"
      + "password: safeharness:DBAtpYCHSx2fPG8MIFQFmA\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SMTP";

  private Class yamlClass = SmtpConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String name = "SMTP" + System.currentTimeMillis();

    // 1. Create SMTP record
    SettingAttribute settingAttributeSaved = createSMTPVerificationProvider(name);
    assertEquals(name, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String name = "SMTP" + System.currentTimeMillis();

    // 1. Create SMTP record
    SettingAttribute settingAttributeSaved = createSMTPVerificationProvider(name);
    testFailureScenario(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  private SettingAttribute createSMTPVerificationProvider(String name) {
    // Generate SMTP connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(name)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(SmtpConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .host(url)
                                                   .port(4403)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .useSSL(true)
                                                   .fromAddress("support@harness.io")
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(collaborationProviderYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(SmtpConfig.class)
        .updateMethodName("setHost")
        .currentFieldValue(url)
        .build();
  }
}
