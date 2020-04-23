package software.wings.delegatetasks.citasks.cik8handler.params;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CIVolumeResponse {
  private Volume volume;
  private VolumeMount volumeMount;
}
