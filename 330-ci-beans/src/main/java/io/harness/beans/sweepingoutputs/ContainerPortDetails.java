package io.harness.beans.sweepingoutputs;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CI;

@Value
@Builder
@TypeAlias("containerPortDetails")
@JsonTypeName("containerPortDetails")
@OwnedBy(CI)
public class ContainerPortDetails implements ExecutionSweepingOutput {
  public static final String PORT_DETAILS = "portDetails";
  Map<String, List<Integer>> portDetails;
  Map<String, String> ctrNameDetails;
  String stageId;
  Map<String, Integer> stepMemDetails;
  Map<String, Integer> stepCpuDetails;

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
