package io.harness.grpc;

import io.harness.grpc.server.Connector;
import lombok.Data;

import java.util.List;

@Data
public class GrpcServerConfig {
  private List<Connector> connectors;
}
