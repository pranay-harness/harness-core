package io.harness.delegate.task.citasks.cik8handler.container;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Volume;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerSpecBuilderResponse {
  private ContainerBuilder containerBuilder;
  private List<Volume> volumes;
  private LocalObjectReference imageSecret;
}
