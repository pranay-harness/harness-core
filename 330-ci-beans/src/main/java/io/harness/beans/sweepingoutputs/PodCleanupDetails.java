package io.harness.beans.sweepingoutputs;

import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("podCleanupDetails")
@JsonTypeName("podCleanupDetails")
public class PodCleanupDetails implements ExecutionSweepingOutput {
  List<String> cleanUpContainerNames;
  Infrastructure infrastructure;
  String podName;
  public static final String CLEANUP_DETAILS = "podCleanupDetails";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Override
  public String getType() {
    return "cleanupContainerDetails";
  }
}
