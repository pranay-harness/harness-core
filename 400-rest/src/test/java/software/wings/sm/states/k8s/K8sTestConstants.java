package software.wings.sm.states.k8s;

public interface K8sTestConstants {
  String VALUES_YAML_WITH_ARTIFACT_REFERENCE = "replicas: 1\n"
      + "image: ${artifact.metadata.image}\n"
      + "dockercfg: ${artifact.source.dockerconfig}";

  String VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE = "replicas: 1\n"
      + "#image: ${artifact.metadata.image}\n"
      + "  #  dockercfg: ${artifact.source.dockerconfig}";

  String VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE = "replicas: 1";
}
