/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.buckets.resources.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;

import java.util.Map;

@OwnedBy(CDP)
public interface GcsResourceService {
  Map<String, String> listBuckets(
      IdentifierRef gcpConnectorRef, String accountId, String orgIdentifier, String projectIdentifier);
}
