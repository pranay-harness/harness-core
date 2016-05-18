package software.wings.beans;

import static software.wings.beans.EnvironmentValue.EnvironmentVariableTypes.HOST_ATTRIBUTES;

import com.google.common.base.MoreObjects;

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

  private String osType;
  private AccessType accessType;
  private ConnectionType connectionType;
  private String key;

  public HostAttributes() {
    super(HOST_ATTRIBUTES);
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
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
    return Objects.hash(osType, accessType, connectionType, key);
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
    return Objects.equals(this.osType, other.osType) && Objects.equals(this.accessType, other.accessType)
        && Objects.equals(this.connectionType, other.connectionType) && Objects.equals(this.key, other.key);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("osType", osType)
        .add("accessType", accessType)
        .add("connectionType", connectionType)
        .add("key", key)
        .toString();
  }

  public static final class HostAttributesBuilder {
    private String osType;
    private AccessType accessType;
    private ConnectionType connectionType;
    private String key;

    private HostAttributesBuilder() {}

    public static HostAttributesBuilder aHostAttributes() {
      return new HostAttributesBuilder();
    }

    public HostAttributesBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
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
      return aHostAttributes()
          .withOsType(osType)
          .withAccessType(accessType)
          .withConnectionType(connectionType)
          .withKey(key);
    }

    public HostAttributes build() {
      HostAttributes hostAttributes = new HostAttributes();
      hostAttributes.setOsType(osType);
      hostAttributes.setAccessType(accessType);
      hostAttributes.setConnectionType(connectionType);
      hostAttributes.setKey(key);
      return hostAttributes;
    }
  }
}
