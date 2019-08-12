package io.harness.grpc;

import lombok.Data;

@Data
public class GrpcServerConfig {
  int plainTextPort;
  int tlsPort;
  String keyFile;
  String certFile;
}
