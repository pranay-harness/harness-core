package io.harness.delegate.task.citasks.cik8handler.k8java.container;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1Volume;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class ContainerSpecBuilderResponse {
  private V1ContainerBuilder containerBuilder;
  private List<V1Volume> volumes;
  private V1LocalObjectReference imageSecret;
}
