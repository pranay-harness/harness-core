/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnvironmentSummary extends AbstractEntitySummary {
  private boolean prod;

  @Builder
  public EnvironmentSummary(String id, String name, String type, boolean prod) {
    super(id, name, type);
    this.prod = prod;
  }
}
