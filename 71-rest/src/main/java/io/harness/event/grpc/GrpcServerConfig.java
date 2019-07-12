package io.harness.event.grpc;

import lombok.Value;

@Value
public class GrpcServerConfig {
  int tlsPort;
  int plainTextPort;
  String keyFile;
  String certFile;
}
