/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.build;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublishedArtifact {
  private String buildNumber;
  private String buildLink;
}
