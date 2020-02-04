package software.wings.service.impl.instana;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanaTagFilter {
  private String name, value;
  private Operator operator;

  public enum Operator { EQUALS, CONTAINS, NOT_EQUAL, NOT_CONTAIN, NOT_EMPTY, IS_EMPTY }
}
