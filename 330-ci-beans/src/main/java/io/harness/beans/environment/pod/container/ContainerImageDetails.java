package io.harness.beans.environment.pod.container;

import io.harness.k8s.model.ImageDetails;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Stores connector identifier to fetch latest image from connector and populate imageDetails.
 */

@Data
@Builder
public class ContainerImageDetails {
  private transient ImageDetails imageDetails;
  @NotEmpty private String connectorIdentifier;
}
