/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.beans;

import static io.harness.NGResourceFilterConstants.PAGE_KEY;
import static io.harness.NGResourceFilterConstants.SIZE_KEY;
import static io.harness.NGResourceFilterConstants.SORT_KEY;

import io.harness.beans.SortOrder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.constraints.Max;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
  @QueryParam(PAGE_KEY) @DefaultValue("0") int pageIndex;
  @QueryParam(SIZE_KEY) @DefaultValue("50") @Max(100) int pageSize;
  @QueryParam(SORT_KEY) List<SortOrder> sortOrders;
}
