package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SecretText;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.SocketConnectivityCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Bean to store all the ldap sso provider configuration details
 *
 * @author Swapnil
 */
@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@FieldNameConstants(innerTypeName = "LdapSettingsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LDAP")
public class LdapSettings extends SSOSettings implements ExecutionCapabilityDemander {
  @NotBlank String accountId;
  @NotNull @Valid LdapConnectionSettings connectionSettings;
  private Boolean useOnlyFallBackMechanism;

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
        secretManager.deleteSecret(accountId, oldEncryptedBindPassword, new HashMap<>(), false);
      }
      SecretText secretText = SecretText.builder()
                                  .value(connectionSettings.getBindPassword())
                                  .hideFromListing(true)
                                  .name(UUID.randomUUID().toString())
                                  .scopedToAccount(true)
                                  .build();
      String encryptedBindPassword = secretManager.saveSecretText(accountId, secretText, false);
      connectionSettings.setEncryptedBindPassword(encryptedBindPassword);
      connectionSettings.setBindPassword(LdapConstants.MASKED_STRING);
    }
  }

  public void decryptFields(
      @NotNull EncryptedDataDetail encryptedDataDetail, @NotNull EncryptionService encryptionService) {
    if (connectionSettings.getBindPassword().equals(LdapConstants.MASKED_STRING)) {
      String bindPassword = new String(encryptionService.getDecryptedValue(encryptedDataDetail, false));
      connectionSettings.setBindPassword(bindPassword);
    }
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }

  @Override
  public SSOType getType() {
    return SSOType.LDAP;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(SocketConnectivityCapabilityGenerator.buildSocketConnectivityCapability(
        connectionSettings.getHost(), Integer.toString(connectionSettings.getPort())));
  }
}
