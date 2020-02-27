package software.wings.service.impl.instana;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ApplicationParamsKeys")
public class InstanaApplicationParams {
  private String hostTagFilter;
  private List<InstanaTagFilter> tagFilters;

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(tagFilters);
  }

  public void validateFields(Map<String, String> errors) {
    if (isEmpty(hostTagFilter)) {
      errors.put("applicationParams." + ApplicationParamsKeys.hostTagFilter, "hostTagFilter is a required field.");
    }
    getTagFilters().forEach(tagFilter -> {
      if (isEmpty(tagFilter.getName())) {
        errors.put("applicationParams.tagFilter.name", "tagFilter.name is a required field.");
      }
      if (isEmpty(tagFilter.getValue())) {
        errors.put("applicationParams.tagFilter.value", "tagFilter.value is a required field.");
      }
      if (tagFilter.getOperator() == null) {
        errors.put("applicationParams.tagFilter.operator", "tagFilter.operator is a required field.");
      }
    });
  }
}
