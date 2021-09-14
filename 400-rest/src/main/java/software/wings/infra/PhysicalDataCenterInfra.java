/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.infra;

import software.wings.beans.infrastructure.Host;

import java.util.List;

public interface PhysicalDataCenterInfra {
  List<String> getHostNames();
  List<Host> getHosts();
  String getLoadBalancerId();
  String getLoadBalancerName();
}
