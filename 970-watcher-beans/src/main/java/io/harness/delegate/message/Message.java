/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.message;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Message {
  String message;
  List<String> params;
  MessengerType fromType;
  String fromProcess;
  long timestamp;
}
