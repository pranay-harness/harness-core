package io.harness.beans.sweepingoutputs;

import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
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
@OwnedBy(HarnessTeam.CI)
@TypeAlias(CODEBASE)
@JsonTypeName(CODEBASE)
public class CodebaseSweepingOutput implements ExecutionSweepingOutput {
  String commitBranch;
  String commitBefore;
  String commitRef;
  String commitSha;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore String uuid;
}
