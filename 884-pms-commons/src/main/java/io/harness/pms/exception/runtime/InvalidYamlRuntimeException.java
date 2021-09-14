/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlNode;

import java.io.IOException;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class InvalidYamlRuntimeException extends RuntimeException {
  YamlNode yamlNode;
  Exception originalException;
  public InvalidYamlRuntimeException(String message) {
    super(message);
  }

  public InvalidYamlRuntimeException(String message, YamlNode yamlNode) {
    super(message);
    this.yamlNode = yamlNode;
  }

  public InvalidYamlRuntimeException(String message, IOException cause) {
    super(message);
    this.originalException = cause;
  }

  public InvalidYamlRuntimeException(String message, Exception cause, YamlNode yamlNode) {
    super(message);
    this.originalException = cause;
    this.yamlNode = yamlNode;
  }
}
