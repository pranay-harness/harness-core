package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@JsonTypeName("CUSTOM")
@Value
@Builder
public class CustomPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.CUSTOM;
}
