package software.wings.yaml.errorhandling;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitProcessingError {
  private String accountId;
  private String message;
  private Long createdAt;
}
