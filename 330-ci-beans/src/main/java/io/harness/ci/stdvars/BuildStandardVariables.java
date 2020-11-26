package io.harness.ci.stdvars;

import io.harness.data.SweepingOutput;
import io.harness.validation.Update;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stepTaskDetails")
public class BuildStandardVariables implements SweepingOutput {
  public static final String BUILD_VARIABLE = "build";
  private GitVariables git;
  private Long number;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
