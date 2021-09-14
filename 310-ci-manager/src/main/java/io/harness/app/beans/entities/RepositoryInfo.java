/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryInfo {
  private String name;
  private long buildCount;
  private double percentSuccess;
  private double successRate;
  private LastRepositoryInfo lastRepository;
  private List<RepositoryBuildInfo> countList;
}
