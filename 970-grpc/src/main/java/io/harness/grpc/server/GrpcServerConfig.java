package io.harness.grpc.server;

import java.util.List;
import lombok.Data;

@Data
public class GrpcServerConfig {
  private List<Connector> connectors;
}
