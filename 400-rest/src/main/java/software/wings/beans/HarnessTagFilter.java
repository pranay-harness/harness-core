package software.wings.beans;

import io.harness.beans.SearchFilter.Operator;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessTagFilterKeys")
public class HarnessTagFilter {
  private boolean matchAll;
  private List<TagFilterCondition> conditions;

  @Data
  @Builder
  public static class TagFilterCondition {
    private String name;
    private HarnessTagType tagType;
    private List<String> values;
    Operator operator;
  }
}
