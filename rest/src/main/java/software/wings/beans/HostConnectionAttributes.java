package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeName("HOST_CONNECTION_ATTRIBUTES")
@Data
@EqualsAndHashCode(callSuper = false)
public class HostConnectionAttributes extends SettingValue implements Encryptable {
  @Attributes(title = "Connection Type", required = true) @NotNull private ConnectionType connectionType;
  @Attributes(title = "Access Type", required = true) @NotNull private AccessType accessType;

  @Attributes(title = "User Name") private String userName;
  @JsonView(JsonViews.Internal.class) @Attributes(title = "Key") @Encrypted private char[] key;
  @SchemaIgnore @NotNull private String accountId;

  @SchemaIgnore private String encryptedKey;

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

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private ConnectionType connectionType;
    private AccessType accessType;
    private char[] key;
    private String encryptedKey;
    private String userName;
    private String accountId;

    private Builder() {}

    /**
     * A host connection attributes.
     *
     * @return the host connection attributes builder
     */
    public static Builder aHostConnectionAttributes() {
      return new Builder();
    }

    /**
     * With connection type.
     *
     * @param connectionType the connection type
     * @return the host connection attributes builder
     */
    public Builder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    /**
     * With access type.
     *
     * @param accessType the access type
     * @return the host connection attributes builder
     */
    public Builder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    /**
     * With key.
     *
     * @param key the key
     * @return the host connection attributes builder
     */
    public Builder withKey(char[] key) {
      this.key = key;
      return this;
    }

    /**
     * With user name.
     *
     * @param userName the user name
     * @return the host connection attributes builder
     */
    public Builder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    /**
     * With accountId.
     *
     * @param accountId the accountId
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withEncyptedKey(String encryptedKey) {
      this.encryptedKey = encryptedKey;
      return this;
    }

    /**
     * But.
     *
     * @return the host connection attributes builder
     */
    public Builder but() {
      return aHostConnectionAttributes()
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withKey(key)
          .withEncyptedKey(encryptedKey)
          .withUserName(userName)
          .withAccountId(accountId);
    }

    /**
     * Builds the.
     *
     * @return the host connection attributes
     */
    public HostConnectionAttributes build() {
      HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
      hostConnectionAttributes.setConnectionType(connectionType);
      hostConnectionAttributes.setAccessType(accessType);
      hostConnectionAttributes.setKey(key);
      hostConnectionAttributes.setEncryptedKey(encryptedKey);
      hostConnectionAttributes.setUserName(userName);
      hostConnectionAttributes.setAccountId(accountId);
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
    public Yaml(
        String type, String harnessApiVersion, String connectionType, String accessType, String userName, String key) {
      super(type, harnessApiVersion);
      this.connectionType = connectionType;
      this.accessType = accessType;
      this.userName = userName;
      this.key = key;
    }
  }
}
