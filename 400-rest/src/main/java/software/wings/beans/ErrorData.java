package software.wings.beans;

import static java.util.Collections.EMPTY_LIST;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorData {
  private Exception exception;
  @Builder.Default private String email = "";
  @Builder.Default private List<BugsnagTab> tabs = EMPTY_LIST;
}
