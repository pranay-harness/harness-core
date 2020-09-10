package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.SocketConnectivityCapabilityGenerator;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Bean to store all the ldap sso provider configuration details
 *
 * @author Swapnil
 */
@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "LdapSettingsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapSettings extends SSOSettings implements ExecutionCapabilityDemander {
  @NotBlank String accountId;
  @NotNull @Valid LdapConnectionSettings connectionSettings;

  /**
   * Keeping the below two attributes only for migration purpose.
   * Will be removed in subsequent release.
   */
  @Deprecated @Valid LdapUserSettings userSettings;

  @Deprecated @Valid LdapGroupSettings groupSettings;

  @Valid List<LdapUserSettings> userSettingsList;

  @Valid List<LdapGroupSettings> groupSettingsList;

  /**
   * Keeping this attribute only for migration purpose.
   * Will be removed in subsequent release.
   * Also, saving this info at present as UI still needs
   * some work to start using collections of groupSettings
   */

  @JsonCreator
  @Builder
  public LdapSettings(@JsonProperty("displayName") String displayName, @JsonProperty("accountId") String accountId,
      @JsonProperty("connectionSettings") LdapConnectionSettings connectionSettings,
      @JsonProperty("userSettingsList") List<LdapUserSettings> userSettingsList,
      @JsonProperty("groupSettingsList") List<LdapGroupSettings> groupSettingsList) {
    super(SSOType.LDAP, displayName, connectionSettings.generateUrl());
    this.accountId = accountId;
    this.connectionSettings = connectionSettings;
    this.userSettingsList = userSettingsList;
    this.groupSettingsList = groupSettingsList;
  }

  public EncryptedDataDetail getEncryptedDataDetails(SecretManager secretManager) {
    return secretManager
        .encryptedDataDetails(
            accountId, LdapConstants.BIND_PASSWORD_KEY, connectionSettings.getEncryptedBindPassword(), null)
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(SocketConnectivityCapabilityGenerator.buildSocketConnectivityCapability(
        connectionSettings.getHost(), Integer.toString(connectionSettings.getPort())));
  }
}
