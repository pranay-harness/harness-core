package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class VerificationProvidersYaml extends GenericYaml {
  @YamlSerialize public List<String> jenkins = new ArrayList<>();
  @YamlSerialize public List<String> appDynamics = new ArrayList<>();
  @YamlSerialize public List<String> splunk = new ArrayList<>();
  @YamlSerialize public List<String> elk = new ArrayList<>();
  @YamlSerialize public List<String> logz = new ArrayList<>();

  public List<String> getJenkins() {
    return jenkins;
  }

  public void setJenkins(List<String> jenkins) {
    this.jenkins = jenkins;
  }

  public List<String> getAppDynamics() {
    return appDynamics;
  }

  public void setAppDynamics(List<String> appDynamics) {
    this.appDynamics = appDynamics;
  }

  public List<String> getSplunk() {
    return splunk;
  }

  public void setSplunk(List<String> splunk) {
    this.splunk = splunk;
  }

  public List<String> getElk() {
    return elk;
  }

  public void setElk(List<String> elk) {
    this.elk = elk;
  }

  public List<String> getLogz() {
    return logz;
  }

  public void setLogz(List<String> logz) {
    this.logz = logz;
  }
}
