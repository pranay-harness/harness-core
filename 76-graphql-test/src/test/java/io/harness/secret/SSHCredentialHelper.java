package io.harness.secret;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import lombok.Data;
import software.wings.beans.Account;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

@Singleton
public class SSHCredentialHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  @Inject SettingsService settingsService;

  @Data
  public static class SSHAuthenticationType {
    String userName;
    int port;
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

  public String CreateSSHCredential() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    HostConnectionAttributes settingValue =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(HostConnectionAttributes.AccessType.KERBEROS)
            .withUserName("userName")
            .withSshPort(22)
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey("key".toCharArray())
            .withSshPassword("sshPassword".toCharArray())
            .withKerberosPassword("kerberosPassword".toCharArray())
            .withKeyPath("keyPath")
            .withKeyless(false)
            .withPassphrase("passphrase".toCharArray())
            .withAuthenticationScheme(HostConnectionAttributes.AuthenticationScheme.SSH_KEY)
            .build();
    settingValue.setSettingType(SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES);
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
