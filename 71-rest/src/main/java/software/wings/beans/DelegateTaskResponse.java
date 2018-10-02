package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.waitnotify.NotifyResponseData;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateTaskResponse {
  private String accountId;
  private NotifyResponseData response;
  private ResponseCode responseCode;

  public enum ResponseCode {
    OK,
    FAILED,
    RETRY_ON_OTHER_DELEGATE,
  }
}
