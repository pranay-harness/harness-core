package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateTaskResponse {
  private String accountId;
  private ResponseData response;
  private ResponseCode responseCode;

  public enum ResponseCode {
    OK,
    FAILED,
    RETRY_ON_OTHER_DELEGATE,
  }
}
