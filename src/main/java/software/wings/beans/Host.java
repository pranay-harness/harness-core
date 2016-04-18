package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;
import java.util.stream.Collectors;

@Entity(value = "hosts", noClassnameStored = true)
public class Host extends Base {
  public enum AccessType {
    SSH,
    SSH_KEY,
    SSH_USER_PASSWD,
    SSH_SU_APP_ACCOUNT,
    SSH_SUDO_APP_ACCOUNT;
  }
  public Host() {}

  public Host(String infraID, String hostName, String osType, AccessType accessType) {
    this.infraID = infraID;
    this.hostName = hostName;
    this.osType = osType;
    this.accessType = accessType;
  }

  @Indexed private String applicationId;

  @Indexed(unique = true) private String hostName;

  private String osType;

  private String ipAddress;

  private int sshPort;
  private String hostAlias;
  private String envUuid;

  private String dcUuid;
  private String ozUuid;
  private AccessType accessType;

  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags;

  private String infraID;

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }
  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
  public String getHostAlias() {
    return hostAlias;
  }

  public void setHostAlias(String hostAlias) {
    this.hostAlias = hostAlias;
  }
  public String getEnvUuid() {
    return envUuid;
  }

  public void setEnvUuid(String envUuid) {
    this.envUuid = envUuid;
  }
  public String getDcUuid() {
    return dcUuid;
  }
  public void setDcUuid(String dcUuid) {
    this.dcUuid = dcUuid;
  }
  public String getOzUuid() {
    return ozUuid;
  }
  public void setOzUuid(String ozUuid) {
    this.ozUuid = ozUuid;
  }
  public AccessType getAccessType() {
    return accessType;
  }
  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  public int getSshPort() {
    return sshPort;
  }
  public void setSshPort(int sshPort) {
    this.sshPort = sshPort;
  }
  public String getApplicationId() {
    return applicationId;
  }
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
  public String getInfraID() {
    return infraID;
  }

  public void setInfraID(String infraID) {
    this.infraID = infraID;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }
  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public String getTagsString() {
    return tags.stream().map(Tag ::getTagString).collect(Collectors.joining(","));
  }
}
