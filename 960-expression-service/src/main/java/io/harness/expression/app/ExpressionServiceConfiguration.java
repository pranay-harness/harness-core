/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.app;

import io.harness.grpc.server.Connector;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExpressionServiceConfiguration {
  private List<Connector> connectors;
  private String secret;
}
