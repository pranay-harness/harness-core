package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
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
  @Attributes(title = "Connection Type", required = true) @NotNull private ConnectionType connectionType;
  @Attributes(title = "Access Type", required = true) @NotNull private AccessType accessType;

  @Attributes(title = "User Name") private String userName;
  @Attributes(title = "Key") @Encrypted private char[] key;
  @SchemaIgnore @NotNull private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;

  private boolean keyless;

  private String keyPath;

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
    KEY_SUDO_APP_USER
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
    private String encryptedKey;
    private boolean keyless;
    private String keyPath;

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

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withKey(char[] key) {
      this.key = key;
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

    public Builder but() {
      return aHostConnectionAttributes()
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withUserName(userName)
          .withKey(key)
          .withAccountId(accountId)
          .withEncryptedKey(encryptedKey)
          .withKeyless(keyless)
          .withKeyPath(keyPath);
    }

    public HostConnectionAttributes build() {
      HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
      hostConnectionAttributes.setConnectionType(connectionType);
      hostConnectionAttributes.setAccessType(accessType);
      hostConnectionAttributes.setUserName(userName);
      hostConnectionAttributes.setKey(key);
      hostConnectionAttributes.setAccountId(accountId);
      hostConnectionAttributes.setEncryptedKey(encryptedKey);
      hostConnectionAttributes.setKeyless(keyless);
      hostConnectionAttributes.setKeyPath(keyPath);
      return hostConnectionAttributes;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends SettingValue.Yaml {
    private String connectionType;
    private String accessType;
    private String userName;
    private String key;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String connectionType, String accessType, String userName,
        String key, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.connectionType = connectionType;
      this.accessType = accessType;
      this.userName = userName;
      this.key = key;
    }
  }
}
