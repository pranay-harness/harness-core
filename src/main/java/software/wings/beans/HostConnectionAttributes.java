package software.wings.beans;

import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeName("HOST_CONNECTION_ATTRIBUTES")
public class HostConnectionAttributes extends SettingValue {
  private ConnectionType connectionType;
  private AccessType accessType;
  private String key;
  private String keyPassphrase;

  /**
   * Instantiates a new host connection attributes.
   */
  public HostConnectionAttributes() {
    super(HOST_CONNECTION_ATTRIBUTES);
  }

  /**
   * Instantiates a new host connection attributes.
   *
   * @param type the type
   */
  public HostConnectionAttributes(SettingVariableTypes type) {
    super(type);
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
  public String getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Gets key passphrase.
   *
   * @return the key passphrase
   */
  public String getKeyPassphrase() {
    return keyPassphrase;
  }

  /**
   * Sets key passphrase.
   *
   * @param keyPassphrase the key passphrase
   */
  public void setKeyPassphrase(String keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(accessType, connectionType, key, keyPassphrase);
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
        && Objects.equals(this.connectionType, other.connectionType) && Objects.equals(this.key, other.key)
        && Objects.equals(this.keyPassphrase, other.keyPassphrase);
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
   * The Class HostConnectionAttributesBuilder.
   */
  public static final class HostConnectionAttributesBuilder {
    private ConnectionType connectionType;
    private AccessType accessType;
    private String key;
    private String keyPassphrase;
    private SettingVariableTypes type;

    private HostConnectionAttributesBuilder() {}

    /**
     * A host connection attributes.
     *
     * @return the host connection attributes builder
     */
    public static HostConnectionAttributesBuilder aHostConnectionAttributes() {
      return new HostConnectionAttributesBuilder();
    }

    /**
     * With connection type.
     *
     * @param connectionType the connection type
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    /**
     * With access type.
     *
     * @param accessType the access type
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    /**
     * With key.
     *
     * @param key the key
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    /**
     * With key passphrase.
     *
     * @param keyPassphrase the key passphrase
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    /**
     * With type.
     *
     * @param type the type
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    /**
     * But.
     *
     * @return the host connection attributes builder
     */
    public HostConnectionAttributesBuilder but() {
      return aHostConnectionAttributes()
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withType(type);
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
      hostConnectionAttributes.setKeyPassphrase(keyPassphrase);
      hostConnectionAttributes.setType(type);
      return hostConnectionAttributes;
    }
  }
}
