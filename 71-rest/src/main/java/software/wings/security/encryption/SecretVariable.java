package software.wings.security.encryption;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(exclude = "value")
public class SecretVariable {
  @NonNull private String name;
  @NonNull private String value;
}
