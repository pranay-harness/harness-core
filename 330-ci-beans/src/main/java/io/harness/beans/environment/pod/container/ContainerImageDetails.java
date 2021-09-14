/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
