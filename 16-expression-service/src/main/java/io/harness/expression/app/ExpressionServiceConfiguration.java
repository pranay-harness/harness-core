package io.harness.expression.app;

import io.harness.grpc.server.Connector;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExpressionServiceConfiguration {
  private List<Connector> connectors;
  private String secret;
}
