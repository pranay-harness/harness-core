/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queryconverter.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GridRequest {
  String entity;

  @Builder.Default List<FieldAggregation> aggregate = new ArrayList<>();

  @Builder.Default List<FieldFilter> where = new ArrayList<>();

  @Builder.Default List<String> groupBy = new ArrayList<>();

  // TODO: This is experimental
  // having (if aggregation is selected)
  @Builder.Default List<FieldFilter> having = new ArrayList<>();

  @Builder.Default List<SortCriteria> orderBy = new ArrayList<>();

  Integer offset;
  Integer limit;
}
