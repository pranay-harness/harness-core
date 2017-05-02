package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Objects;

/**
 * Created by anubhaw on 5/17/16.
 */
@JsonTypeName("BASTION_HOST_CONNECTION_ATTRIBUTES")
public class BastionConnectionAttributes extends HostConnectionAttributes {
  @NotEmpty private String hostName;

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
   * The type Builder.
   */
  public static final class Builder {
    private ConnectionType connectionType;
    private AccessType accessType;
    private String hostName;
    private String key;
    private String userName;

    private Builder() {}

    /**
     * A bastion connection attributes builder.
     *
     * @return the builder
     */
    public static Builder aBastionConnectionAttributes() {
      return new Builder();
    }

    /**
     * With connection type builder.
     *
     * @param connectionType the connection type
     * @return the builder
     */
    public Builder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    /**
     * With access type builder.
     *
     * @param accessType the access type
     * @return the builder
     */
    public Builder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With key builder.
     *
     * @param key the key
     * @return the builder
     */
    public Builder withKey(String key) {
      this.key = key;
      return this;
    }

    /**
     * With user name builder.
     *
     * @param userName the user name
     * @return the builder
     */
    public Builder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBastionConnectionAttributes()
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withHostName(hostName)
          .withKey(key)
          .withUserName(userName);
    }

    /**
     * Build bastion connection attributes.
     *
     * @return the bastion connection attributes
     */
    public BastionConnectionAttributes build() {
      BastionConnectionAttributes bastionConnectionAttributes = new BastionConnectionAttributes();
      bastionConnectionAttributes.setConnectionType(connectionType);
      bastionConnectionAttributes.setAccessType(accessType);
      bastionConnectionAttributes.setHostName(hostName);
      bastionConnectionAttributes.setKey(key);
      bastionConnectionAttributes.setUserName(userName);
      return bastionConnectionAttributes;
    }
  }
}
