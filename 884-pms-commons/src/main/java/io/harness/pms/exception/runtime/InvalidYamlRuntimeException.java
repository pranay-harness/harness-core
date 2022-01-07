/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
