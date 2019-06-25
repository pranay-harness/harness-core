package software.wings.infra;

public interface KubernetesInfrastructure {
  String getClusterName();
  String getNamespace();
  String getReleaseName();
}
