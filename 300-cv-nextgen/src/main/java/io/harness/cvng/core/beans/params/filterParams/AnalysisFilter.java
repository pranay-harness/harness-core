/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.params.filterParams;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Data
public abstract class AnalysisFilter {
  String filter;
  List<String> healthSourceIdentifiers;

  public boolean filterByHealthSourceIdentifiers() {
    return isNotEmpty(healthSourceIdentifiers);
  }

  public boolean filterByFilter() {
    return isNotEmpty(filter);
  }
}
