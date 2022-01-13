/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;

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

  public String createWinRMCredential(String name) {
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
    settingValue.setSettingType(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES);
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(name)
                                            .withValue(settingValue)
                                            .withAccountId(accountId)
                                            .withCategory(SettingAttribute.SettingCategory.SETTING)
                                            .build();
    SettingAttribute savedSettingAttribute =
        settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
    return savedSettingAttribute.getUuid();
  }
}
