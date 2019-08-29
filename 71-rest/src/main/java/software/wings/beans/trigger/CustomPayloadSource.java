package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@JsonTypeName("CUSTOM")
@Value
@Builder
public class CustomPayloadSource implements PayloadSource {
  @NotNull private Type type = Type.CUSTOM;
}
