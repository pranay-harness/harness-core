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
}
