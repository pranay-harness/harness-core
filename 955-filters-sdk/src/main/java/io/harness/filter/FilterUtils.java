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
