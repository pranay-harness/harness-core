package software.wings.delegatetasks.citasks.cik8handler.container;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Volume;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContainerSpecBuilderResponse {
  private ContainerBuilder containerBuilder;
  private List<Volume> volumes;
  private LocalObjectReference imageSecret;
}