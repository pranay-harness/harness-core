package software.wings.beans;

import static java.util.Collections.EMPTY_LIST;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ErrorData {
  private Exception exception;
  @Builder.Default private String email = "";
  @Builder.Default private List<BugsnagTab> tabs = EMPTY_LIST;
}
