/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.beans.Service;
import software.wings.verification.CVConfiguration;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CVEnabledService {
  private Service service;
  List<CVConfiguration> cvConfig;
  private String appName;
  private String appId;
}
