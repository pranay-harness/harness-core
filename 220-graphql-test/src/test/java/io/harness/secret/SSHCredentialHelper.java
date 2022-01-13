/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SecretGenerator;
import io.harness.scm.SecretName;
import io.harness.shell.AccessType;
import io.harness.shell.AuthenticationScheme;
import io.harness.shell.KerberosConfig;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationMethodOutput;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;

@Singleton
@OwnedBy(PL)
public class SSHCredentialHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  @Inject SettingsService settingsService;
  @Inject private SecretGenerator secretGenerator;
  @Inject private ApplicationGenerator applicationGenerator;

  @Data
  public static class SSHAuthenticationType {
    String userName;
    int port;
    QLSSHAuthenticationMethodOutput sshAuthenticationMethod;
  }

  @Data
  public static class SSHCredentialResult {
    String id;
    String name;
    SSHAuthenticationType authenticationType;
  }

  @Data
  public static class SSHResult {
    String clientMutationId;
    SSHCredentialResult secret;
  }

  @Data
  public static class KerberosAuthenticationType {
    String principal;
    String realm;
    int port;
  }

  @Data
  public static class KerberosCredentialResult {
    String id;
    String name;
    KerberosAuthenticationType authenticationType;
  }

  @Data
  public static class KerberosResult {
    String clientMutationId;
    KerberosCredentialResult secret;
  }

  public String createSSHCredential(String name) {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Application application =
        applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    final String secretId = secretGenerator.ensureStored(owners, SecretName.builder().value("pcf_password").build());
    HostConnectionAttributes settingValue =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(AccessType.KERBEROS)
            .withUserName("userName")
            .withSshPort(22)
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey(secretId.toCharArray())
            .withSshPassword(secretId.toCharArray())
            .withKerberosPassword(secretId.toCharArray())
            .withKerberosConfig(KerberosConfig.builder().principal("principal").realm("realm").build())
            .withKeyPath("keyPath")
            .withKeyless(false)
            .withPassphrase(secretId.toCharArray())
            .withAuthenticationScheme(AuthenticationScheme.SSH_KEY)
            .build();
    settingValue.setSettingType(SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES);
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
