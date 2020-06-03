package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
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
@Slf4j
public class AddRestrictionsToSecrets implements Migration {
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

      PageResponse<EncryptedData> pageResponse = secretManager.listSecrets(accountId, pageRequest, null, null, true);
      List<EncryptedData> secretTextList = pageResponse.getResponse();

      secretTextList.forEach(secretText -> {
        if (secretText.getUsageRestrictions() != null
            && isNotEmpty(secretText.getUsageRestrictions().getAppEnvRestrictions())) {
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

        secretManager.updateUsageRestrictionsForSecretOrFile(accountId, secretText.getUuid(), usageRestrictions, false);
      });

      logger.info("Migration of secrets done successfully");
    } catch (Exception e) {
      logger.error("Migration of secrets failed", e);
    }
  }
}
