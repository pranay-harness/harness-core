package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeName("HOST_CONNECTION_ATTRIBUTES")
public class HostConnectionAttributes extends SettingValue implements Encryptable {
  @Attributes(title = "Connection Type", required = true) @NotNull private ConnectionType connectionType;
  @Attributes(title = "Access Type", required = true) @NotNull private AccessType accessType;

  @Attributes(title = "User Name") private String userName;
  @JsonView(JsonViews.Internal.class) @Attributes(title = "Key") @Encrypted private char[] key;
  @SchemaIgnore @NotNull private String accountId;
  @Attributes(title = "Encrypted Fields", required = true)
  private final static List<String> encryptedFields = Arrays.asList("key");

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
   * Gets the list of fields that are encrypted for use in the UI
   * @return List of field names
   */
  @JsonIgnore
  public List<String> getEncryptedFields() {
    return encryptedFields;
  }

  /**
   * Gets access type.
   *
   * @return the access type
   */
  public AccessType getAccessType() {
    return accessType;
  }

  /**
   * Sets access type.
   *
   * @param accessType the access type
   */
  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  /**
   * Gets connection type.
   *
   * @return the connection type
   */
  public ConnectionType getConnectionType() {
    return connectionType;
  }

  /**
   * Sets connection type.
   *
   * @param connectionType the connection type
   */
  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public char[] getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(char[] key) {
    this.key = key;
  }

  /**
   * Getter for property 'userName'.
   *
   * @return Value for property 'userName'.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Setter for property 'userName'.
   *
   * @param userName Value to set for property 'userName'.
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(accessType, connectionType, key, userName);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final HostConnectionAttributes other = (HostConnectionAttributes) obj;
    return Objects.equals(this.accessType, other.accessType)
        && Objects.equals(this.connectionType, other.connectionType) && Arrays.equals(this.key, other.key)
        && Objects.equals(this.userName, other.userName);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accessType", accessType)
        .add("connectionType", connectionType)
        .add("userName", userName)
        .toString();
  }

  /**
   * The Enum AccessType.
   */
  public enum AccessType {
    /**
     * User password access type.
     */
    USER_PASSWORD, /**
                    * User password su app user access type.
                    */
    USER_PASSWORD_SU_APP_USER, /**
                                * User password sudo app user access type.
                                */
    USER_PASSWORD_SUDO_APP_USER, /**
                                  * Key access type.
                                  */
    KEY, /**
          * Key su app user access type.
          */
    KEY_SU_APP_USER, /**
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
      hostConnectionAttributes.setUserName(userName);
      hostConnectionAttributes.setAccountId(accountId);
      return hostConnectionAttributes;
    }
  }
}
