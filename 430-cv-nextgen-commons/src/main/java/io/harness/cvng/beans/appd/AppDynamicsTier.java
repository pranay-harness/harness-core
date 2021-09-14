/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.appd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsTier implements Comparable<AppDynamicsTier> {
  long id;
  String name;

  @Override
  public int compareTo(@NotNull AppDynamicsTier o) {
    return name.compareTo(o.name);
  }
}
