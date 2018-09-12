package software.wings.beans.sso;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;
import ro.fortsoft.pf4j.Extension;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Bean to store all the ldap sso provider configuration details
 *
 * @author Swapnil
 */
@Extension
@EqualsAndHashCode(callSuper = true)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapSettings extends SSOSettings {
  @NotBlank String accountId;
  @NotNull @Valid LdapConnectionSettings connectionSettings;
  @Valid LdapUserSettings userSettings;
  @Valid LdapGroupSettings groupSettings;

  @JsonCreator
  public LdapSettings(@JsonProperty("displayName") String displayName, @JsonProperty("accountId") String accountId,
      @JsonProperty("connectionSettings") LdapConnectionSettings connectionSettings,
      @JsonProperty("userSettings") LdapUserSettings userSettings,
      @JsonProperty("groupSettings") LdapGroupSettings groupSettings) {
    super(SSOType.LDAP, displayName, connectionSettings.generateUrl());
    this.accountId = accountId;
    this.connectionSettings = connectionSettings;
    this.userSettings = userSettings;
    this.groupSettings = groupSettings;
  }

  public EncryptedDataDetail getEncryptedDataDetails(SecretManager secretManager) {
    return secretManager
        .encryptedDataDetails(accountId, LdapConstants.BIND_PASSWORD_KEY, connectionSettings.getEncryptedBindPassword())
        .get();
  }

  public void encryptFields(SecretManager secretManager) {
    if (!connectionSettings.getBindPassword().equals(LdapConstants.MASKED_STRING)) {
      String oldEncryptedBindPassword = connectionSettings.getEncryptedBindPassword();
      if (isNotEmpty(oldEncryptedBindPassword)) {
        secretManager.deleteSecretUsingUuid(oldEncryptedBindPassword);
      }
      String encryptedBindPassword = secretManager.encrypt(accountId, connectionSettings.getBindPassword(), null);
      connectionSettings.setEncryptedBindPassword(encryptedBindPassword);
      connectionSettings.setBindPassword(LdapConstants.MASKED_STRING);
    }
  }

  public void decryptFields(
      @NotNull EncryptedDataDetail encryptedDataDetail, @NotNull EncryptionService encryptionService) {
    if (connectionSettings.getBindPassword().equals(LdapConstants.MASKED_STRING)) {
      try {
        String bindPassword = new String(encryptionService.getDecryptedValue(encryptedDataDetail));
        connectionSettings.setBindPassword(bindPassword);
      } catch (IOException e) {
        throw new WingsException("Unable to decrypt the field bindPassword");
      }
    }
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }
}
