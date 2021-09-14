/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfRouteInfo {
  private PcfRouteType type;
  private String domain;

  // PCF_ROUTE_TYPE_HTTP
  private String hostName;
  private String path;

  // For PCF_ROUTE_TYPE_TCP
  private String port;
}
