package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebHookToken {
  private String webHookToken;
  private String httpMethod;
  private String payload;
}
