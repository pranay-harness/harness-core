package io.harness.ci.stdvars;

import io.harness.pms.sdk.core.data.SweepingOutput;
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
public class BuildStandardVariables implements SweepingOutput {
  public static final String BUILD_VARIABLE = "build";
  GitVariables git;
  Long number;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Override
  public String getType() {
    return "buildStandardVariables";
  }
}
