/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.params;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
@NoArgsConstructor
public class PageParams {
  @QueryParam("pageNumber") @DefaultValue("0") @NonNull int page;
  @QueryParam("pageSize") @DefaultValue("10") @NonNull int size;
}
