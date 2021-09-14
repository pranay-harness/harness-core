/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serviceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ServiceInfoService {
  boolean updateLatest(String serviceId, String version);

  List<ServiceInfo> getAllServices();

  List<String> getAllVersions(String service);
}
