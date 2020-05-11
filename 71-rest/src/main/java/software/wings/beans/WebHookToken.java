package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class WebHookToken {
  private String webHookToken;
  private String httpMethod;
  private String payload;
}
