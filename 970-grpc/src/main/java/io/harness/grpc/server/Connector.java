/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
