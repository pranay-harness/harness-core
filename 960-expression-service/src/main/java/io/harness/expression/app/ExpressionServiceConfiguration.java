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
