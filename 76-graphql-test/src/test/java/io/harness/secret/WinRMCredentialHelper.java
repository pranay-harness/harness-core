package io.harness.secret;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import lombok.Data;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

@Singleton
public class WinRMCredentialHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  @Inject SettingsService settingsService;

  @Data
  public static class WinRMSecret {
    String name;
    String id;
    int port;
    boolean skipCertCheck;
    boolean useSSL;
    String userName;
    QLUsageScope usageScope;
  }

  @Data
  public static class UpdateWinRMResult {
    String clientMutationId;
    WinRMSecret secret;
  }

  public String createWinRMCredential() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    WinRmConnectionAttributes settingValue =
        WinRmConnectionAttributes.builder()
            .username("userName")
            .password("password".toCharArray())
            .authenticationScheme(WinRmConnectionAttributes.AuthenticationScheme.NTLM)
            .port(22)
            .skipCertChecks(true)
            .accountId(accountId)
            .useSSL(true)
            .domain("")
            .build();
    settingValue.setSettingType(SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES);
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName("secretName")
                                            .withValue(settingValue)
                                            .withAccountId(accountId)
                                            .withCategory(SettingAttribute.SettingCategory.SETTING)
                                            .build();
    SettingAttribute savedSettingAttribute =
        settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
    return savedSettingAttribute.getUuid();
  }
}
