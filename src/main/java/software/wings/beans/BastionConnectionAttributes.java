package software.wings.beans;

import static software.wings.beans.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by anubhaw on 5/17/16.
 */
public class BastionConnectionAttributes extends HostConnectionAttributes {
  private String hostName;

  public BastionConnectionAttributes() {
    super(BASTION_HOST_CONNECTION_ATTRIBUTES);
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(hostName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final BastionConnectionAttributes other = (BastionConnectionAttributes) obj;
    return Objects.equals(this.hostName, other.hostName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }

  public static final class BastionConnectionAttributesBuilder {
    private String hostName;
    private ConnectionType connectionType;
    private AccessType accessType;
    private String key;
    private String keyPassphrase;
    private SettingVariableTypes type;

    private BastionConnectionAttributesBuilder() {}

    public static BastionConnectionAttributesBuilder aBastionConnectionAttributes() {
      return new BastionConnectionAttributesBuilder();
    }

    public BastionConnectionAttributesBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public BastionConnectionAttributesBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public BastionConnectionAttributesBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public BastionConnectionAttributesBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    public BastionConnectionAttributesBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    public BastionConnectionAttributesBuilder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    public BastionConnectionAttributesBuilder but() {
      return aBastionConnectionAttributes()
          .withHostName(hostName)
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withType(type);
    }

    public BastionConnectionAttributes build() {
      BastionConnectionAttributes bastionConnectionAttributes = new BastionConnectionAttributes();
      bastionConnectionAttributes.setHostName(hostName);
      bastionConnectionAttributes.setConnectionType(connectionType);
      bastionConnectionAttributes.setAccessType(accessType);
      bastionConnectionAttributes.setKey(key);
      bastionConnectionAttributes.setKeyPassphrase(keyPassphrase);
      bastionConnectionAttributes.setType(type);
      return bastionConnectionAttributes;
    }
  }
}
