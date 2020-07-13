package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class SecretVarParams {
  @NotNull private String secretName; // Name of the secret
  @NotNull private String secretKey; // Name of key in the secret
}
