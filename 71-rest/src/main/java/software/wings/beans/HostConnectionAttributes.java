package software.wings.beans;

import static software.wings.audit.ResourceType.CONNECTION_ATTRIBUTES;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.Encrypted;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeName("HOST_CONNECTION_ATTRIBUTES")
@Data
@EqualsAndHashCode(callSuper = false)
public class HostConnectionAttributes extends SettingValue implements EncryptableSetting {
  public static final String KEY_KEY = "key";
  public static final String KEY_PASSPHRASE = "passphrase";
  public static final String KEY_SSH_PASSWORD = "sshPassword";
  public static final String KEY_KERBEROS_PASSWORD = "kerberosPassword";

  @Attributes(title = "Connection Type", required = true) @NotNull private ConnectionType connectionType;
  @Attributes(title = "Access Type", required = true) @NotNull private AccessType accessType;

  @Attributes(title = "User Name") private String userName;
  @Attributes(title = "SSH Password") @Encrypted private char[] sshPassword;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSshPassword;
  @Attributes(title = "SSH Port") private Integer sshPort = 22;
  @Attributes(title = "Key") @Encrypted private char[] key;
  @SchemaIgnore @NotNull private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;

  private boolean keyless;
  private String keyPath;
  @Attributes(title = "Pass Phrase") @Encrypted private char[] passphrase;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassphrase;

  @Attributes(title = "Auth Scheme") private AuthenticationScheme authenticationScheme = SSH_KEY;

  @Override
  public String fetchResourceCategory() {
    return CONNECTION_ATTRIBUTES.name();
  }

  public enum AuthenticationScheme { SSH_KEY, KERBEROS, HTTP_PASSWORD }
  @Attributes private KerberosConfig kerberosConfig;
  @Attributes(title = "Kerberos Password") @Encrypted private char[] kerberosPassword;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKerberosPassword;

  /**
   * Instantiates a new host connection attributes.
   */
  public HostConnectionAttributes() {
    super(HOST_CONNECTION_ATTRIBUTES.name());
  }

  /**
   * Instantiates a new host connection attributes.
   *
   * @param type the type
   */
  public HostConnectionAttributes(SettingVariableTypes type) {
    super(type.name());
  }

  /**
   * The Enum AccessType.
   */
  public enum AccessType {
    /**
     * User password access type.
     */
    USER_PASSWORD,
    /**
     * User password su app user access type.
     */
    USER_PASSWORD_SU_APP_USER,
    /**
     * User password sudo app user access type.
     */
    USER_PASSWORD_SUDO_APP_USER,
    /**
     * Key access type.
     */
    KEY,
    /**
     * Key su app user access type.
     */
    KEY_SU_APP_USER,
    /**
     * Key sudo app user access type.
     */
    KEY_SUDO_APP_USER,
    /**
     * Kerberos Access Type.
     */
    KERBEROS
  }

  /**
   * The Enum ConnectionType.
   */
  public enum ConnectionType {
    /**
     * Ssh connection type.
     */
    SSH
  }

  public static final class Builder {
    private ConnectionType connectionType;
    private AccessType accessType;
    private char[] key;
    private String accountId;
    private String userName;
    private Integer sshPort = 22;
    private String encryptedKey;
    private boolean keyless;
    private String keyPath;
    private AuthenticationScheme authenticationScheme = SSH_KEY;
    private KerberosConfig kerberosConfig;
    private char[] passphrase;
    private String encryptedPassphrase;
    private char[] sshPassword;
    private String encryptedSshPassword;

    private Builder() {}

    public static Builder aHostConnectionAttributes() {
      return new Builder();
    }

    public Builder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public Builder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public Builder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    public Builder withSshPort(Integer sshPort) {
      this.sshPort = sshPort;
      return this;
    }

    public Builder withKey(char[] key) {
      this.key = key == null ? null : key.clone();
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withEncryptedKey(String encryptedKey) {
      this.encryptedKey = encryptedKey;
      return this;
    }

    public Builder withKeyless(boolean keyless) {
      this.keyless = keyless;
      return this;
    }

    public Builder withKeyPath(String keyPath) {
      this.keyPath = keyPath;
      return this;
    }

    public Builder withAuthenticationScheme(AuthenticationScheme authenticationScheme) {
      this.authenticationScheme = authenticationScheme;
      return this;
    }

    public Builder withKerberosConfig(KerberosConfig kerberosConfig) {
      this.kerberosConfig = kerberosConfig;
      return this;
    }

    public Builder withPassphrase(char[] passphrase) {
      this.passphrase = passphrase == null ? null : passphrase.clone();
      return this;
    }

    public Builder withEncryptedPassphrase(String encryptedPassphrase) {
      this.encryptedPassphrase = encryptedPassphrase;
      return this;
    }

    public Builder withSshPassword(char[] sshPassword) {
      this.sshPassword = sshPassword == null ? null : sshPassword.clone();
      return this;
    }

    public Builder withEncryptedSshPassword(String encryptedSshPassword) {
      this.encryptedSshPassword = encryptedSshPassword;
      return this;
    }

    public Builder but() {
      return aHostConnectionAttributes()
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withUserName(userName)
          .withSshPort(sshPort)
          .withKey(key)
          .withAccountId(accountId)
          .withEncryptedKey(encryptedKey)
          .withKeyless(keyless)
          .withKeyPath(keyPath)
          .withAuthenticationScheme(authenticationScheme)
          .withKerberosConfig(kerberosConfig)
          .withPassphrase(passphrase)
          .withEncryptedPassphrase(encryptedPassphrase)
          .withSshPassword(sshPassword)
          .withEncryptedSshPassword(encryptedSshPassword);
    }

    public HostConnectionAttributes build() {
      HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
      hostConnectionAttributes.setConnectionType(connectionType);
      hostConnectionAttributes.setAccessType(accessType);
      hostConnectionAttributes.setUserName(userName);
      hostConnectionAttributes.setSshPort(sshPort);
      hostConnectionAttributes.setKey(key);
      hostConnectionAttributes.setAccountId(accountId);
      hostConnectionAttributes.setEncryptedKey(encryptedKey);
      hostConnectionAttributes.setKeyless(keyless);
      hostConnectionAttributes.setKeyPath(keyPath);
      hostConnectionAttributes.setAuthenticationScheme(authenticationScheme);
      hostConnectionAttributes.setKerberosConfig(kerberosConfig);
      hostConnectionAttributes.setPassphrase(passphrase);
      hostConnectionAttributes.setEncryptedPassphrase(encryptedPassphrase);
      hostConnectionAttributes.setSshPassword(sshPassword);
      hostConnectionAttributes.setEncryptedSshPassword(encryptedSshPassword);
      return hostConnectionAttributes;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends SettingValue.Yaml {
    private String connectionType;
    private String accessType;
    private String userName;
    private String key;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String connectionType, String accessType, String userName,
        String key, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.connectionType = connectionType;
      this.accessType = accessType;
      this.userName = userName;
      this.key = key;
    }
  }
}
