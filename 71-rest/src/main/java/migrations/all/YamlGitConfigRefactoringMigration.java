package migrations.all;

import com.google.inject.Inject;

import io.harness.data.validator.EntityNameValidator;
import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Account;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

public class YamlGitConfigRefactoringMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigRefactoringMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;
  @Inject private SettingsService settingsService;
  @Inject private AccountService accountService;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;
  @Inject private YamlPushService yamlPushService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  private String jsonUsageRestrictionString = "{\"appEnvRestrictions\":"
      + "[{\"appFilter\":{\"type\":\"GenericEntityFilter\",\"ids\":null,\"filterType\":\"ALL\"},"
      + "\"envFilter\":{\"type\":\"EnvFilter\",\"ids\":null,\"filterTypes\":[\"NON_PROD\"]}},"
      + "{\"appFilter\":{\"type\":\"GenericEntityFilter\",\"ids\":null,\"filterType\":\"ALL\"},"
      + "\"envFilter\":{\"type\":\"EnvFilter\",\"ids\":null,\"filterTypes\":[\"PROD\"]}}]}";

  @Override
  public void migrate() {
    logger.info("Retrieving all YamlGitConfigs");
    UsageRestrictions defaultUsageRestrictions =
        usageRestrictionsService.getUsageRestrictionsFromJson(jsonUsageRestrictionString);

    try (HIterator<YamlGitConfig> yamlGitConfigHIterator =
             new HIterator<>(wingsPersistence.createQuery(YamlGitConfig.class).fetch())) {
      while (yamlGitConfigHIterator.hasNext()) {
        YamlGitConfig yamlGitConfig = yamlGitConfigHIterator.next();
        encryptionService.decrypt(
            yamlGitConfig, secretManager.getEncryptionDetails(yamlGitConfig, "_GLOBAL_APP_ID_", null));
        logger.info("Creating GitConnector for yaml from YamlGitConfig {}", yamlGitConfig.getUuid());
        GitConfig gitConfig = getGitConfig(yamlGitConfig);
        Account account = accountService.get(yamlGitConfig.getAccountId());
        SettingAttribute settingAttributeForGit = wingsPersistence.saveAndGet(SettingAttribute.class,
            SettingAttribute.Builder.aSettingAttribute()
                .withAccountId(yamlGitConfig.getAccountId())
                .withValue(gitConfig)
                .withCategory(Category.CONNECTOR)
                .withName(
                    EntityNameValidator.getMappedString(account.getAccountName() + "_default_Git_Connector_For_Yaml"))
                .withUsageRestrictions(defaultUsageRestrictions)
                .build());

        logger.info("Created GitConnector from YamlGitConfig {}", yamlGitConfig.getUuid());
        logger.info("Updating YamlGitConfig with newly created GitConnector");
        yamlGitConfig.setGitConnectorId(settingAttributeForGit.getUuid());
        /* Not calling yamlGitServiceImpl.save() as it will initiate validate call and gitFullSync.
         * We currently do not have yaml representation of gitConnector, so it wont have any need to perform gitSync.
         * */
        wingsPersistence.saveAndGet(YamlGitConfig.class, yamlGitConfig);
        logger.info("Updated YamlGitConfig with newly created GitConnector {}", settingAttributeForGit.getUuid());
      }
    }
  }

  public GitConfig getGitConfig(YamlGitConfig yamlGitConfig) {
    return GitConfig.builder()
        .accountId(yamlGitConfig.getAccountId())
        .repoUrl(yamlGitConfig.getUrl())
        .username(yamlGitConfig.getUsername())
        .password(yamlGitConfig.getPassword())
        .encryptedPassword(yamlGitConfig.getEncryptedPassword())
        .sshSettingId(yamlGitConfig.getSshSettingId())
        .keyAuth(yamlGitConfig.isKeyAuth())
        .branch(yamlGitConfig.getBranchName().trim())
        .webhookToken(yamlGitConfig.getWebhookToken())
        .description("Default Yaml git connector for account")
        .build();
  }

  private void decrypt(SettingValue settingValue) {
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);
    encryptionService.decrypt((EncryptableSetting) settingValue, encryptedDataDetails);
  }
}
