package software.wings.beans.ci.pod;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretVariableDTO implements DecryptableEntity {
  public enum Type { TEXT, FILE }
  @NotNull String name;
  @NotNull Type type;
  @NotNull @SecretReference SecretRefData secret;
}
