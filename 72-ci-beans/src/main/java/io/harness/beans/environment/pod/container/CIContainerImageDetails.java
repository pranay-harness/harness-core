package io.harness.beans.environment.pod.container;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.container.ImageDetails;

@Data
@Value
@Builder
public class CIContainerImageDetails {
  private ImageDetails imageDetails;
  @NotEmpty private String settingId;
}
