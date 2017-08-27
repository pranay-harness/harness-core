package software.wings.beans.infrastructure;

public class InstanceKey {
  private String hostName;
  private String infraMappingId;

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    InstanceKey that = (InstanceKey) o;

    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null)
      return false;
    return infraMappingId != null ? infraMappingId.equals(that.infraMappingId) : that.infraMappingId == null;
  }

  @Override
  public int hashCode() {
    int result = hostName != null ? hostName.hashCode() : 0;
    result = 31 * result + (infraMappingId != null ? infraMappingId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "InstanceKey{"
        + "hostName='" + hostName + '\'' + ", infraMappingId='" + infraMappingId + '\'' + '}';
  }

  public static final class Builder {
    private String hostName;
    private String infraMappingId;

    private Builder() {}

    public static Builder anInstanceKey() {
      return new Builder();
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public InstanceKey build() {
      InstanceKey instanceKey = new InstanceKey();
      instanceKey.setHostName(hostName);
      instanceKey.setInfraMappingId(infraMappingId);
      return instanceKey;
    }
  }
}
