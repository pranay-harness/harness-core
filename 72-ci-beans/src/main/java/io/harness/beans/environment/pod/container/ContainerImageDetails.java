package io.harness.beans.environment.pod.container;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.container.ImageDetails;

/**
 * Stores connector identifier to fetch latest image from connector and populate imageDetails.
 */

@Data
@Value
@Builder
public class ContainerImageDetails {
  private transient ImageDetails imageDetails;
  @NotEmpty private String connectorIdentifier;
}
