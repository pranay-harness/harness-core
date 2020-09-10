package software.wings.security.authentication.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import lombok.Value;

import java.util.List;

@OwnedBy(PL)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationStatus {
  private Boolean success;
  private String hostname;
  @JsonProperty("challenge_ts") private String challengeTs;
  @JsonProperty("error-codes") private List<String> errorCodes;
}
