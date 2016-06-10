package software.wings.beans;

import static software.wings.beans.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/17/16.
 */
@JsonTypeName("BASTION_HOST_CONNECTION_ATTRIBUTES")
public class BastionConnectionAttributes extends HostConnectionAttributes {
  private String hostName;

  /**
   * Instantiates a new bastion connection attributes.
   */
  public BastionConnectionAttributes() {
    super(BASTION_HOST_CONNECTION_ATTRIBUTES);
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.HostConnectionAttributes#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(hostName);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.HostConnectionAttributes#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }

  /**
   * The Class BastionConnectionAttributesBuilder.
   */
  public static final class BastionConnectionAttributesBuilder {
    private String hostName;
    private ConnectionType connectionType;
    private AccessType accessType;
    private String key;
    private String keyPassphrase;
    private SettingVariableTypes type;

    private BastionConnectionAttributesBuilder() {}

    /**
     * A bastion connection attributes.
     *
     * @return the bastion connection attributes builder
     */
    public static BastionConnectionAttributesBuilder aBastionConnectionAttributes() {
      return new BastionConnectionAttributesBuilder();
    }

    /**
     * With host name.
     *
     * @param hostName the host name
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With connection type.
     *
     * @param connectionType the connection type
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    /**
     * With access type.
     *
     * @param accessType the access type
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    /**
     * With key.
     *
     * @param key the key
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    /**
     * With key passphrase.
     *
     * @param keyPassphrase the key passphrase
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    /**
     * With type.
     *
     * @param type the type
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    /**
     * But.
     *
     * @return the bastion connection attributes builder
     */
    public BastionConnectionAttributesBuilder but() {
      return aBastionConnectionAttributes()
          .withHostName(hostName)
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withType(type);
    }

    /**
     * Builds the.
     *
     * @return the bastion connection attributes
     */
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
