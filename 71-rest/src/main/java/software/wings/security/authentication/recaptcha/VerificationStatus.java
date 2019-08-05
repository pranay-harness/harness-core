package software.wings.security.authentication.recaptcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationStatus {
  private Boolean success;
  private String hostname;
  @JsonProperty("challenge_ts") private String challengeTs;
  @JsonProperty("error-codes") private List<String> errorCodes;
}
