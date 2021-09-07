package io.harness.ci.stdvars;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.RecasterAlias;
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
@TypeAlias("buildStandardVariables")
@JsonTypeName("buildStandardVariables")
@OwnedBy(CI)
@RecasterAlias("io.harness.ci.stdvars.BuildStandardVariables")
public class BuildStandardVariables implements ExecutionSweepingOutput {
  public static final String BUILD_VARIABLE = "build";
  GitVariables git;
  Long number;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
