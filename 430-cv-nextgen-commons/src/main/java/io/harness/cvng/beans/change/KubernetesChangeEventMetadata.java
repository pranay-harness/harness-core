package io.harness.cvng.beans.change;

public class KubernetesChangeEventMetadata extends ChangeEventMetadata {
  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.KUBERNETES;
  }
}
