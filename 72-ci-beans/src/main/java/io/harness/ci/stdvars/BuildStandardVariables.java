package io.harness.ci.stdvars;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.SweepingOutput;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class BuildStandardVariables implements SweepingOutput {
  public static final String BUILD_VARIABLE = "build";
  private GitVariables git;
  private Long number;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
