package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by anubhaw on 5/17/16.
 */
public class BastionHostAttributes extends HostAttributes {
  private String hostName;

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
    final BastionHostAttributes other = (BastionHostAttributes) obj;
    return Objects.equals(this.hostName, other.hostName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }
}
