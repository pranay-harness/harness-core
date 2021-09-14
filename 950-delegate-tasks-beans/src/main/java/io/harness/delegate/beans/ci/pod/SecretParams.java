/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretParams {
  public enum Type { FILE, TEXT }
  private String value;
  private String secretKey;
  private Type type;
}
