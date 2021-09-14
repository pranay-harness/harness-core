/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.server;

import java.util.List;
import lombok.Data;

@Data
public class GrpcServerConfig {
  private List<Connector> connectors;
}
