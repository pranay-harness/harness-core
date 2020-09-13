package io.harness.beans.sweepingoutputs;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.SweepingOutput;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class StepTaskDetails implements SweepingOutput {
  private Map<String, String> taskIds;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
