package software.wings.beans;

import static software.wings.beans.EnvironmentValue.EnvironmentVariableTypes.HOST_ATTRIBUTES;

import java.util.Objects;

/**
 * Created by anubhaw on 5/16/16.
 */
public class HostAttributes extends EnvironmentValue {
  public enum AccessType {
    PASSWORD,
    PASSWORD_SU_APP_ACCOUNT,
    PASSWORD_SUDO_APP_ACCOUNT,
    KEY,
    KEY_SU_APP_ACCOUNT,
    KEY_SUDO_APP_ACCOUNT
  }

  public enum ConnectionType { SSH }

  private AccessType accessType;
  private ConnectionType connectionType;
  private String key;

  public HostAttributes() {
    super(HOST_ATTRIBUTES);
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

  @Override
  public int hashCode() {
    return Objects.hash(accessType, connectionType, key);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final HostAttributes other = (HostAttributes) obj;
    return Objects.equals(this.accessType, other.accessType)
        && Objects.equals(this.connectionType, other.connectionType) && Objects.equals(this.key, other.key);
  }

  public static final class HostAttributesBuilder {
    private AccessType accessType;
    private ConnectionType connectionType;
    private String key;

    private HostAttributesBuilder() {}

    public static HostAttributesBuilder aHostAttributes() {
      return new HostAttributesBuilder();
    }

    public HostAttributesBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public HostAttributesBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public HostAttributesBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    public HostAttributesBuilder but() {
      return aHostAttributes().withAccessType(accessType).withConnectionType(connectionType).withKey(key);
    }

    public HostAttributes build() {
      HostAttributes hostAttributes = new HostAttributes();
      hostAttributes.setAccessType(accessType);
      hostAttributes.setConnectionType(connectionType);
      hostAttributes.setKey(key);
      return hostAttributes;
    }
  }
}
