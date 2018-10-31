package migrations.all;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.SettingAttribute.NAME_KEY;

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
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

public class YamlGitConfigRefactoringMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigRefactoringMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;
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

        EncryptedData encryptedData = null;
        if (yamlGitConfig.getEncryptedPassword() != null) {
          encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                              .filter("accountId", yamlGitConfig.getAccountId())
                              .filter("_id", yamlGitConfig.getEncryptedPassword())
                              .get();
        }

        Account account = accountService.get(yamlGitConfig.getAccountId());

        String settingAttributeForGitName = account.getAccountName() + "_default_Git_Connector_For_Yaml";
        SettingAttribute savedSettingAttribute = wingsPersistence.createQuery(SettingAttribute.class)
                                                     .filter(ACCOUNT_ID_KEY, yamlGitConfig.getAccountId())
                                                     .filter(NAME_KEY, settingAttributeForGitName)
                                                     .filter("category", Category.CONNECTOR)
                                                     .get();

        if (savedSettingAttribute == null) {
          logger.info("Creating GitConnector for yaml from YamlGitConfig {}", yamlGitConfig.getUuid());
          GitConfig gitConfig = getGitConfig(yamlGitConfig, encryptedData);

          SettingAttribute settingAttributeForGit = wingsPersistence.saveAndGet(SettingAttribute.class,
              SettingAttribute.Builder.aSettingAttribute()
                  .withAccountId(yamlGitConfig.getAccountId())
                  .withValue(gitConfig)
                  .withCategory(Category.CONNECTOR)
                  .withName(EntityNameValidator.getMappedString(settingAttributeForGitName))
                  .withUsageRestrictions(defaultUsageRestrictions)
                  .build());

          logger.info("Created GitConnector from YamlGitConfig {}", yamlGitConfig.getUuid());
          logger.info("Updating YamlGitConfig with newly created GitConnector");

          if (encryptedData != null) {
            encryptedData.getParentIds().add(settingAttributeForGit.getUuid());
            wingsPersistence.saveAndGet(EncryptedData.class, encryptedData);
          }

          /* Not calling yamlGitServiceImpl.save() as it will initiate validate call and gitFullSync.
           * We currently do not have yaml representation of gitConnector, so it wont have any need to perform gitSync.
           * */
          wingsPersistence.updateField(
              YamlGitConfig.class, yamlGitConfig.getUuid(), "gitConnectorId", settingAttributeForGit.getUuid());
          logger.info("Updated YamlGitConfig with newly created GitConnector {}", settingAttributeForGit.getUuid());
        }
      }
    }
  }

  public GitConfig getGitConfig(YamlGitConfig yamlGitConfig, EncryptedData encryptedData) {
    GitConfig gitConfig = GitConfig.builder()
                              .accountId(yamlGitConfig.getAccountId())
                              .repoUrl(yamlGitConfig.getUrl())
                              .username(yamlGitConfig.getUsername())
                              .sshSettingId(yamlGitConfig.getSshSettingId())
                              .keyAuth(yamlGitConfig.isKeyAuth())
                              .branch(yamlGitConfig.getBranchName().trim())
                              .webhookToken(yamlGitConfig.getWebhookToken())
                              .description("Default Yaml git connector for account")
                              .build();

    if (encryptedData != null) {
      String encryptedPassword = new StringBuilder()
                                     .append(encryptedData.getEncryptionType().getYamlName())
                                     .append(":")
                                     .append(yamlGitConfig.getEncryptedPassword())
                                     .toString();
      gitConfig.setEncryptedPassword(encryptedPassword);
    }

    return gitConfig;
  }

  private void decrypt(SettingValue settingValue) {
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);
    encryptionService.decrypt((EncryptableSetting) settingValue, encryptedDataDetails);
  }
}
