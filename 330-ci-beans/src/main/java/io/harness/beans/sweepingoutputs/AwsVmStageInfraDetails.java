package io.harness.beans.sweepingoutputs;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
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
@TypeAlias("awsVmStageInfraDetails")
@JsonTypeName("awsVmStageInfraDetails")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.sweepingoutputs.AwsVmStageInfraDetails")
public class AwsVmStageInfraDetails implements StageInfraDetails {
  String poolId;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @Builder.Default @NotNull private Type type = Type.AWS_VM;

  @Override
  public StageInfraDetails.Type getType() {
    return type;
  }
}
