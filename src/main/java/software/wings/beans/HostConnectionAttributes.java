package software.wings.beans;

import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import java.util.Objects;

/**
 * Created by anubhaw on 5/16/16.
 */
public class HostConnectionAttributes extends SettingValue {
  private ConnectionType connectionType;
  private AccessType accessType;
  private String key;
  private String keyPassphrase;

  public enum AccessType {
    USER_PASSWORD,
    USER_PASSWORD_SU_APP_USER,
    USER_PASSWORD_SUDO_APP_USER,
    KEY,
    KEY_SU_APP_USER,
    KEY_SUDO_APP_USER
  }

  public enum ConnectionType { SSH }

  public HostConnectionAttributes() {
    super(HOST_CONNECTION_ATTRIBUTES);
  }

  public HostConnectionAttributes(SettingVariableTypes type) {
    super(type);
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getKeyPassphrase() {
    return keyPassphrase;
  }

  public void setKeyPassphrase(String keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessType, connectionType, key, keyPassphrase);
  }

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

  public static final class HostConnectionAttributesBuilder {
    private AccessType accessType;
    private ConnectionType connectionType;
    private String key;
    private String keyPassphrase;
    private SettingVariableTypes type;

    private HostConnectionAttributesBuilder() {}

    public static HostConnectionAttributesBuilder aHostConnectionAttributes() {
      return new HostConnectionAttributesBuilder();
    }

    public HostConnectionAttributesBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public HostConnectionAttributesBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public HostConnectionAttributesBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    public HostConnectionAttributesBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    public HostConnectionAttributesBuilder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    public HostConnectionAttributesBuilder but() {
      return aHostConnectionAttributes()
          .withAccessType(accessType)
          .withConnectionType(connectionType)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withType(type);
    }

    public HostConnectionAttributes build() {
      HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
      hostConnectionAttributes.setAccessType(accessType);
      hostConnectionAttributes.setConnectionType(connectionType);
      hostConnectionAttributes.setKey(key);
      hostConnectionAttributes.setKeyPassphrase(keyPassphrase);
      hostConnectionAttributes.setType(type);
      return hostConnectionAttributes;
    }
  }
}
