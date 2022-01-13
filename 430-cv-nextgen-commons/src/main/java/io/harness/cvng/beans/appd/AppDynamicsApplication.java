/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
