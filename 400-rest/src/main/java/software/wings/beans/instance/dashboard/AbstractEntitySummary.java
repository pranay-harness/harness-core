/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class AbstractEntitySummary {
  private String id;
  private String name;
  private String type;

  public AbstractEntitySummary(String id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }
}
