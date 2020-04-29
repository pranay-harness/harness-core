package software.wings.beans;

import io.harness.beans.SearchFilter.Operator;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

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
