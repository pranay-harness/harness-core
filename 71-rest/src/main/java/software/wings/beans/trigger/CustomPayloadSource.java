package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@JsonTypeName("CUSTOM")
@Value
@Builder
public class CustomPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.CUSTOM;
}
