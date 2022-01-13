/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.filter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SearchFilter;

import com.google.api.client.util.Lists;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DEL)
public class FilterUtils {
  public static Object[] getFiltersForSearchTerm(String searchTerm, SearchFilter.Operator op, String... fieldNames) {
    Object[] fieldValues = {searchTerm};
    List<SearchFilter> filters = Lists.newArrayList();

    for (String fieldName : fieldNames) {
      filters.add(SearchFilter.builder().fieldName(fieldName).op(op).fieldValues(fieldValues).build());
    }

    return filters.toArray();
  }
}
