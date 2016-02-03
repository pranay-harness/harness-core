package software.wings.beans;

public class ServiceInstance {
  private String name;
  private String description;
  private String port;
  private String healthPort;
  private String svcName;
  private String envName;

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getPort() {
    return port;
  }
  public void setPort(String port) {
    this.port = port;
  }
  public String getHealthPort() {
    return healthPort;
  }
  public void setHealthPort(String healthPort) {
    this.healthPort = healthPort;
  }
  public String getEnvName() {
    return envName;
  }
  public void setEnvName(String envName) {
    this.envName = envName;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public String getSvcName() {
    return svcName;
  }
  public void setSvcName(String svcName) {
    this.svcName = svcName;
  }
}
