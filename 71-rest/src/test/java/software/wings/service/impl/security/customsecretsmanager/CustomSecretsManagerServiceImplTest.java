package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;
import static software.wings.beans.command.CommandType.START;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.WingsTestConstants.DOMAIN;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NoResultFoundException;
import io.harness.exception.UnexpectedException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretVariable;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.CustomSecretsManagerService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomSecretsManagerServiceImplTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Inject @InjectMocks private TemplateService templateService;
  @Inject @InjectMocks private TemplateGalleryService templateGalleryService;
  @Inject @InjectMocks private CustomSecretsManagerService customSecretsManagerService;

  @Before
  public void setup() {
    initMocks(this);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(GLOBAL_ACCOUNT_ID);
    when(accountService.get(GLOBAL_ACCOUNT_ID)).thenReturn(account);
    templateGalleryService.loadHarnessGallery();
  }

  private String obtainTemplate(ScriptType scriptType) {
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(scriptType.name())
                                                  .scriptString("echo ${var1}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();
    Template template =
        Template.builder()
            .templateObject(shellScriptTemplate)
            .folderPath("Harness/Tomcat Commands")
            .gallery(HARNESS_GALLERY)
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .name("Sample Script")
            .variables(Collections.singletonList(aVariable().type(TEXT).name("var1").mandatory(true).build()))
            .build();
    return templateService.save(template).getUuid();
  }

  private String obtainCommandTemplate() {
    SshCommandTemplate sshCommandTemplate =
        SshCommandTemplate.builder()
            .commandType(START)
            .commandUnits(Collections.singletonList(anExecCommandUnit()
                                                        .withName("Start")
                                                        .withCommandPath("/home/xxx/tomcat")
                                                        .withCommandString("bin/startup.sh")
                                                        .build()))
            .build();

    Template template = Template.builder()
                            .templateObject(sshCommandTemplate)
                            .name("name")
                            .description(TEMPLATE_DESC)
                            .folderPath("Harness/Tomcat Commands")
                            .keywords(ImmutableSet.of(TEMPLATE_CUSTOM_KEYWORD))
                            .gallery(HARNESS_GALLERY)
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .build();
    return templateService.save(template).getUuid();
  }

  private String obtainSshSettingAttribute() {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.SETTING)
            .withName("hostConnectionAttrs")
            .withAccountId(GLOBAL_ACCOUNT_ID)
            .withValue(aHostConnectionAttributes()
                           .withAccessType(HostConnectionAttributes.AccessType.USER_PASSWORD)
                           .withAccountId(GLOBAL_ACCOUNT_ID)
                           .build())
            .build();
    return wingsPersistence.save(settingAttribute);
  }

  private String obtainWinrmSettingAttribute() {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingAttribute.SettingCategory.SETTING)
                                            .withName("winrmConnectionAttr")
                                            .withAccountId(GLOBAL_ACCOUNT_ID)
                                            .withValue(WinRmConnectionAttributes.builder()
                                                           .accountId(GLOBAL_ACCOUNT_ID)
                                                           .authenticationScheme(NTLM)
                                                           .username(USER_NAME)
                                                           .password(PASSWORD)
                                                           .domain(DOMAIN)
                                                           .port(22)
                                                           .useSSL(true)
                                                           .skipCertChecks(true)
                                                           .build())
                                            .build();
    return wingsPersistence.save(settingAttribute);
  }

  private Set<SecretVariable> obtainTestVariables(String connectorId) {
    Set<SecretVariable> testVariables = new HashSet<>();
    testVariables.add(SecretVariable.builder().name("var1").value("value").build());
    if (!isEmpty(connectorId)) {
      testVariables.add(SecretVariable.builder().name("connectorId").value(connectorId).build());
    }
    return testVariables;
  }

  private List<String> obtainDelegateSelectors() {
    return Arrays.asList("test1", "test2");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldPass() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isNull();
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("/tmp");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldFail_TestParamsMissing() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = new HashSet<>();

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(new ArrayList<>())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldFail_MissingTemplate() {
    Set<SecretVariable> testVariables = new HashSet<>();

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .delegateSelectors(new ArrayList<>())
                                            .templateId(generateUuid())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldFail_IncorrectTemplate() {
    String templateId = obtainCommandTemplate();
    Set<SecretVariable> testVariables = new HashSet<>();

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .delegateSelectors(new ArrayList<>())
                                            .templateId(templateId)
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldFail_IncorrectVariables() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = new HashSet<>();
    testVariables.add(SecretVariable.builder().name("incorrectKey").value("value").build());

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .delegateSelectors(new ArrayList<>())
                                            .templateId(templateId)
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerExecuteOnDelegate_shouldFail_IncorrectName() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager@")
                                            .delegateSelectors(new ArrayList<>())
                                            .templateId(templateId)
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithSSHConnectorNotTemplatized_shouldSucceed() {
    String connectorId = obtainSshSettingAttribute();
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isEqualTo(config.getHost());
    assertThat(savedConfig.getConnectorId()).isEqualTo(config.getConnectorId());
    assertThat(savedConfig.getCommandPath()).isEqualTo("/tmp");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithWinRMConnectorNotTemplatized_shouldSucceed() {
    String connectorId = obtainWinrmSettingAttribute();
    String templateId = obtainTemplate(ScriptType.POWERSHELL);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isEqualTo(config.getHost());
    assertThat(savedConfig.getConnectorId()).isEqualTo(config.getConnectorId());
    assertThat(savedConfig.getCommandPath()).isEqualTo("%TEMP%");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorNotTemplatized_shouldFail_ConnectorNotFound() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(new ArrayList<>())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(generateUuid())
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorNotTemplatized_shouldFail_WrongConnectorType1() {
    String connectorId = obtainWinrmSettingAttribute();
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(new ArrayList<>())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorNotTemplatized_shouldFail_WrongConnectorType2() {
    String connectorId = obtainSshSettingAttribute();
    String templateId = obtainTemplate(ScriptType.POWERSHELL);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(new ArrayList<>())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorNotTemplatized_shouldFail_MissingTargetHost() {
    String connectorId = obtainSshSettingAttribute();
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(new ArrayList<>())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorTemplatized_shouldSucceed() {
    String connectorId = obtainWinrmSettingAttribute();
    String templateId = obtainTemplate(ScriptType.POWERSHELL);
    Set<SecretVariable> testVariables = obtainTestVariables(connectorId);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(true)
                                            .host("app.harness.io")
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isEqualTo(config.getHost());
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("%TEMP%");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveSecretsManagerWithConnectorTemplatized_shouldFail_missingConnectorInParams() {
    String templateId = obtainTemplate(ScriptType.POWERSHELL);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(true)
                                            .host("app.harness.io")
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isEqualTo(config.getHost());
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("%TEMP%");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void updateSecretsManager_shouldSucceed() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isNull();
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("/tmp");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();

    config.setUuid(savedConfig.getUuid());
    config.setName("Edited name");

    configId = customSecretsManagerService.updateSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    savedConfig = wingsPersistence.get(CustomSecretsManagerConfig.class, configId);
    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isNull();
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("/tmp");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test(expected = NoResultFoundException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void updateSecretsManager_shouldFail_ConfigNotFound() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    config.setUuid(generateUuid());
    customSecretsManagerService.updateSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void updateSecretsManager_shouldFail_IdisNull() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    customSecretsManagerService.updateSecretsManager(GLOBAL_ACCOUNT_ID, config);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void getSecretsManager_shouldPass() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    CustomSecretsManagerConfig savedConfig = customSecretsManagerService.getSecretsManager(GLOBAL_ACCOUNT_ID, configId);

    assertThat(savedConfig.getName()).isEqualTo(config.getName());
    assertThat(savedConfig.getTemplateId()).isEqualTo(config.getTemplateId());
    assertThat(savedConfig.getDelegateSelectors()).isEqualTo(config.getDelegateSelectors());
    assertThat(savedConfig.isExecuteOnDelegate()).isEqualTo(config.isExecuteOnDelegate());
    assertThat(savedConfig.isConnectorTemplatized()).isEqualTo(config.isConnectorTemplatized());
    assertThat(savedConfig.getHost()).isNull();
    assertThat(savedConfig.getConnectorId()).isNull();
    assertThat(savedConfig.getCommandPath()).isEqualTo("/tmp");
    assertThat(savedConfig.getTestVariables()).isEqualTo(testVariables);
    assertThat(savedConfig.getCustomSecretsManagerShellScript()).isNotNull();
    assertThat(savedConfig.getRemoteHostConnector()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void deleteSecretsManager_shouldPass() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    boolean isDeleted = customSecretsManagerService.deleteSecretsManager(GLOBAL_ACCOUNT_ID, configId);
    assertThat(isDeleted).isTrue();
  }

  @Test(expected = SecretManagementException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void deleteSecretsManager_shouldFail_SecretsFound() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    String configId = customSecretsManagerService.saveSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(configId).isNotNull();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .name("name")
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .kmsId(configId)
                                      .encryptionType(CUSTOM)
                                      .build();
    wingsPersistence.save(encryptedData);
    customSecretsManagerService.deleteSecretsManager(GLOBAL_ACCOUNT_ID, configId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void validateSecretsManagerExecuteOnDelegate_shouldPass() {
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(true)
                                            .isConnectorTemplatized(false)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    boolean valid = customSecretsManagerService.validateSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(valid).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void validateSecretsManagerWithConnector_shouldPass() {
    String connectorId = obtainSshSettingAttribute();
    String templateId = obtainTemplate(ScriptType.BASH);
    Set<SecretVariable> testVariables = obtainTestVariables(null);

    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(templateId)
                                            .delegateSelectors(obtainDelegateSelectors())
                                            .executeOnDelegate(false)
                                            .isConnectorTemplatized(false)
                                            .host("app.harness.io")
                                            .connectorId(connectorId)
                                            .testVariables(testVariables)
                                            .build();
    config.setDefault(false);
    boolean valid = customSecretsManagerService.validateSecretsManager(GLOBAL_ACCOUNT_ID, config);
    assertThat(valid).isTrue();
  }
}
