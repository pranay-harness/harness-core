package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionStatusResponse {
  private String status;
}
