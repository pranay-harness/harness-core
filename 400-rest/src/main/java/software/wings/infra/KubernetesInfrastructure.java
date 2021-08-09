package software.wings.infra;

public interface KubernetesInfrastructure extends ContainerInfrastructure {
  String getNamespace();
  String getReleaseName();
}
