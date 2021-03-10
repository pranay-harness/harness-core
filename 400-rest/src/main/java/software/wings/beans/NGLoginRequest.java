package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGLoginRequest {
  private String authorization;
  private String userName;
}

