package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.EnvFilter;
import software.wings.security.EnvFilter.FilterType;
import software.wings.security.GenericEntityFilter;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.List;

/**
 * Migration script to add restrictions to the existing secrets and config files for iHerb.
 * This script is meant to be idempotent, so it could be run any number of times.
 * @author rktummala on 6/20/18
 */
public class AddRestrictionsToSecrets implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddRestrictionsToSecrets.class);
  @Inject private SecretManager secretManager;
  @Inject private AccountService accountService;

  @Override
  public void migrate() {
    try {
      // We are only doing this migration for iHerb since they have asked for this behavior and they have 100s of
      // secrets.
      logger.info("Starting to migrate secrets for iherb");

      Account account = accountService.getByName("iHerb");
      if (account == null) {
        logger.error("Cannot locate iherb account");
        return;
      }

      String accountId = account.getUuid();

      PageRequest<EncryptedData> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("accountId", Operator.EQ, accountId)
              .addFilter("type", Operator.IN,
                  new Object[] {SettingVariableTypes.SECRET_TEXT.name(), SettingVariableTypes.CONFIG_FILE.name()})
              .build();

      PageResponse<EncryptedData> pageResponse = secretManager.listSecrets(accountId, pageRequest, null, null);
      List<EncryptedData> secretTextList = pageResponse.getResponse();

      secretTextList.forEach(secretText -> {
        if (secretText.getUsageRestrictions() != null) {
          return;
        }

        if (isEmpty(secretText.getAppIds())) {
          return;
        }

        GenericEntityFilter appFilter = GenericEntityFilter.builder()
                                            .ids(Sets.newHashSet(secretText.getAppIds()))
                                            .filterType(GenericEntityFilter.FilterType.SELECTED)
                                            .build();
        EnvFilter envFilter =
            EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.NON_PROD, FilterType.PROD)).build();
        AppEnvRestriction appEnvRestriction =
            AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
        UsageRestrictions usageRestrictions =
            UsageRestrictions.builder().appEnvRestrictions(Sets.newHashSet(appEnvRestriction)).build();

        secretManager.updateUsageRestrictionsForSecretOrFile(accountId, secretText.getUuid(), usageRestrictions);
      });

      logger.info("Migration of secrets done successfully");
    } catch (Exception e) {
      logger.error("Migration of secrets failed", e);
    }
  }
}
