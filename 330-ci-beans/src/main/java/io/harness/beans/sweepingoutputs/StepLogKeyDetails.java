package io.harness.beans.sweepingoutputs;

import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stepLogKeyDetails")
@JsonTypeName("stepLogKeyDetails")
public class StepLogKeyDetails implements ExecutionSweepingOutput {
  Map<String, List<String>> logKeys;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Override
  public String getType() {
    return "stepLogKeyDetails";
  }
}
