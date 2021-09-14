/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s.resources.gcp.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.k8s.resources.gcp.GcpResponseDTO;

@OwnedBy(CDP)
public interface GcpResourceService {
  GcpResponseDTO getClusterNames(
      IdentifierRef gcpConnectorRef, String accountId, String orgIdentifier, String projectIdentifier);
}
