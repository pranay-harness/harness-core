/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.server;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
public class Connector {
  int port;
  boolean secure;
  String certFilePath;
  String keyFilePath;

  @Builder
  public Connector(@JsonProperty("port") int port, @JsonProperty("secure") boolean secure,
      @JsonProperty("certFilePath") String certFilePath, @JsonProperty("keyFilePath") String keyFilePath) {
    checkArgument(!secure || certFilePath != null && keyFilePath != null, "secure requires keyFilePath & certFilePath");
    this.port = port;
    this.secure = secure;
    this.certFilePath = certFilePath;
    this.keyFilePath = keyFilePath;
  }
}
