package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorData {
  private Exception exception;
  @Builder.Default private String email = "";
}
