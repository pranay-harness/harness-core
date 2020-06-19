package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretKeyParams {
  @NotNull private String secretName; // Name of the secret
  @NotNull private String key; // Name of key in the secret
}
