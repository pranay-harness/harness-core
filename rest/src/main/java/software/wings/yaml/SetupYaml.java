package software.wings.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class SetupYaml extends GenericYaml {
  @YamlSerialize public List<String> applications = new ArrayList<>();
  @YamlSerialize public CloudProvidersYaml cloudProviders;
  @YamlSerialize public List<String> artifactServers = new ArrayList<>();
  @YamlSerialize public CollaborationProvidersYaml collaborationProviders;
  @YamlSerialize public LoadBalancersYaml loadBalancers;
  @YamlSerialize public VerificationProvidersYaml verificationProviders;

  @JsonIgnore
  public List<String> getAppNames() {
    return applications;
  }

  public void setAppNames(List<String> appNames) {
    this.applications = appNames;
  }

  @JsonIgnore
  public CloudProvidersYaml getCloudProviders() {
    return cloudProviders;
  }

  public void setCloudProviders(CloudProvidersYaml cloudProviders) {
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

  public CollaborationProvidersYaml getCollaborationProviders() {
    return collaborationProviders;
  }

  public void setCollaborationProviders(CollaborationProvidersYaml collaborationProviders) {
    this.collaborationProviders = collaborationProviders;
  }

  public LoadBalancersYaml getLoadBalancers() {
    return loadBalancers;
  }

  public void setLoadBalancers(LoadBalancersYaml loadBalancers) {
    this.loadBalancers = loadBalancers;
  }

  public VerificationProvidersYaml getVerificationProviders() {
    return verificationProviders;
  }

  public void setVerificationProviders(VerificationProvidersYaml verificationProviders) {
    this.verificationProviders = verificationProviders;
  }
}
