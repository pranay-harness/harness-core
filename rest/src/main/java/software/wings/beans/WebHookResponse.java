package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebHookResponse {
  private String requestId;
  private String status;
  private String error;
  private String uiUrl;
  private String apiUrl;
}
