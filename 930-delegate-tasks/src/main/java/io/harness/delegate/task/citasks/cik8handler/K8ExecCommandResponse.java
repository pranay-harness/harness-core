/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler;

import java.io.ByteArrayOutputStream;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Data
@Builder
public class K8ExecCommandResponse {
  private ByteArrayOutputStream outputStream;
  private ExecCommandStatus execCommandStatus;
}
