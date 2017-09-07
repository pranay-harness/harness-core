package software.wings.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class SetupYaml extends GenericYaml {
  @YamlSerialize public List<String> applications = new ArrayList<>();
  @YamlSerialize public List<YamlSubList> cloudProviders = new ArrayList<>();
  @YamlSerialize public List<String> artifactServers = new ArrayList<>();
  @YamlSerialize public List<String> collaborationProviders = new ArrayList<>();
  @YamlSerialize public List<String> loadBalancers = new ArrayList<>();
  @YamlSerialize public List<String> verificationProviders = new ArrayList<>();

  @JsonIgnore
  public List<String> getAppNames() {
    return applications;
  }

  public void setAppNames(List<String> appNames) {
    this.applications = appNames;
  }

  @JsonIgnore
  public List<YamlSubList> getCloudProviders() {
    return cloudProviders;
  }

  public void setCloudProviders(List<YamlSubList> cloudProviders) {
    this.cloudProviders = cloudProviders;
  }

  @JsonIgnore
  public List<String> getArtifactServerNames() {
    return artifactServers;
  }

  public void setArtifactServerNames(List<String> artifactServerNames) {
    this.artifactServers = artifactServerNames;
  }

  public void addArtifactServerName(String artifactServerName) {
    artifactServers.add(artifactServerName);
  }

  @JsonIgnore
  public List<String> getCollaborationProviderNames() {
    return collaborationProviders;
  }

  public void setCollaborationProviderNames(List<String> collaborationProviderNames) {
    this.collaborationProviders = collaborationProviderNames;
  }

  @JsonIgnore
  public List<String> getLoadBalancerNames() {
    return loadBalancers;
  }

  public void setLoadBalancerNames(List<String> loadBalancerNames) {
    this.loadBalancers = loadBalancerNames;
  }

  @JsonIgnore
  public List<String> getVerificationProviderNames() {
    return verificationProviders;
  }

  public void setVerificationProviderNames(List<String> verificationProviderNames) {
    this.verificationProviders = verificationProviderNames;
  }
}
