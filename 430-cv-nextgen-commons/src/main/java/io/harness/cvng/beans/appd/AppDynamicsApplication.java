/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.appd;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppDynamicsApplication implements Comparable<AppDynamicsApplication> {
  String name;
  long id;

  @Override
  public int compareTo(@NotNull AppDynamicsApplication o) {
    return name.compareTo(o.name);
  }
}
