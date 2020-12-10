package io.harness.beans.sweepingoutputs;

import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stepTaskDetails")
@JsonTypeName("stepTaskDetails")
public class StepTaskDetails implements SweepingOutput {
  Map<String, String> taskIds;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Override
  public String getType() {
    return "stepTaskDetails";
  }
}
