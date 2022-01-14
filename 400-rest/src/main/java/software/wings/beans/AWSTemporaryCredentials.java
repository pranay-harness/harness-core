package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class AWSTemporaryCredentials {
  @JsonProperty("Code") String code;
  @JsonProperty("LastUpdated") String lastUpdated;
  @JsonProperty("Type") String type;
  @JsonProperty("AccessKeyId") String accessKeyId;
  @JsonProperty("SecretAccessKey") String secretKey;
  @JsonProperty("Token") String token;
  @JsonProperty("Expiration") String expiration;
}
