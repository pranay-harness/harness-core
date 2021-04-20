package io.harness.beans.outcomes;

import io.harness.pms.sdk.core.data.Outcome;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("podDetailsOutcome")
@JsonTypeName("podDetailsOutcome")
public class LiteEnginePodDetailsOutcome implements Outcome {
  String ipAddress;
  public static final String POD_DETAILS_OUTCOME = "podDetailsOutcome";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Override
  public String getType() {
    return "podDetailsOutcome";
  }
}
