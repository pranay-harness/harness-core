/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.key;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class AwsLambdaInstanceKey extends ServerlessInstanceKey {
  private String functionName;
  private String functionVersion;

  @Builder
  public AwsLambdaInstanceKey(String functionName, String functionVersion) {
    this.functionName = functionName;
    this.functionVersion = functionVersion;
  }
}
