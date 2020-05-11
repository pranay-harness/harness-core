package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class WebHookResponse {
  private String requestId;
  private String status;
  private String error;
  private String uiUrl;
  private String apiUrl;
  private String message;
}
