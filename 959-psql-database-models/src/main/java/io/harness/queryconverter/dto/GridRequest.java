/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
