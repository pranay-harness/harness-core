package io.harness.grpc.server;

import lombok.Data;

import java.util.List;

@Data
public class GrpcServerConfig {
  private List<Connector> connectors;
}
