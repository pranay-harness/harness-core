package io.harness.beans.environment.pod.container;

import io.harness.k8s.model.ImageDetails;

import lombok.Builder;
import lombok.Data;

/**
 * Stores connector identifier to fetch latest image from connector and populate imageDetails.
 */

@Data
@Builder
public class ContainerImageDetails {
  private ImageDetails imageDetails;
  private String connectorIdentifier;
}
