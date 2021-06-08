package io.harness.argo.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArgoApp {
  String apiVersion = "argoproj.io/v1alpha1";
  String kind = "Application";
  Map<String, Object> metadata = new HashMap<>();
  Map<String, Object> spec = new HashMap<>();
  Map<String, Object> status = new HashMap<>();
  Map<String, Object> operation = new HashMap<>();

  public String health() {
    final Map<String, Object> sync = (Map<String, Object>) status.get("health");
    return (String) sync.get("status");
  }

  public String syncStatus() {
    final Map<String, Object> sync = (Map<String, Object>) status.get("sync");
    return (String) sync.get("status");
  }

  public String incomingRevision() {
    final Map<String, Object> sync = (Map<String, Object>) status.get("sync");
    return (String) sync.get("revision");
  }

  public String syncedRevision() {
    final Map<String, Object> operationState = (Map<String, Object>) status.get("operationState");
    final Map<String, Object> operation = (Map<String, Object>) operationState.get("operation");
    final Map<String, Object> sync = (Map<String, Object>) operation.get("sync");
    return (String) sync.get("revision");
  }

  public String previousSyncStatus() {
    final Map<String, Object> operationState = (Map<String, Object>) status.get("operationState");
    return (String) operationState.get("phase");
  }

  private void setName(String name) {
    metadata.put("name", name);
  }

  private void setDestination(String namespace, String server) {
    setNamespace(namespace);
    setServer(server);
  }

  private void setServer(String server) {
    spec.put("server", server);
  }

  private void setNamespace(String namespace) {
    spec.put("namespace", namespace);
  }

  private void setGitSource(String path, String repoUrl, String targetRevision) {
    setGitPath(path);
    setGitRepoUrl(repoUrl);
    setTargetRevision(targetRevision);
  }

  private void setTargetRevision(String targetRevision) {
    spec.put("targetRevision", targetRevision);
  }

  private void setGitRepoUrl(String repoUrl) {
    spec.put("repoURL", repoUrl);
  }

  private void setGitPath(String path) {
    spec.put("path", path);
  }

  private void setSyncPolicy(boolean validate, boolean applyOutOfSyncOnly) {
    List<String> options = new ArrayList<>();
    options.add("Validate=" + validate);
    options.add("ApplyOutOfSyncOnly=" + applyOutOfSyncOnly);
    Map<String, Object> syncPolicy = new HashMap<>();
    syncPolicy.put("syncOptions", options);
    spec.put("syncPolicy", syncPolicy);
  }

  public static ArgoApp ArgoApp(ArgoAppRequest appRequest) {
    ArgoApp argoApp = new ArgoApp();
    final ArgoAppRequest.Destination destination = appRequest.getDestination();
    final ArgoAppRequest.Source source = appRequest.getSource();
    final ArgoAppRequest.SyncPolicyOptions syncPolicyOptions = appRequest.getSyncPolicyOptions();
    argoApp.setDestination(destination.getNamespace(), destination.getServer());
    argoApp.setGitSource(source.getPath(), source.getRepoUrl(), source.getTargetRevision());
    argoApp.setSyncPolicy(syncPolicyOptions.isValidate(), syncPolicyOptions.isApplyOutOfSyncOnly());
    return argoApp;
  }
}
