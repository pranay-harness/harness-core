package io.harness.beans.yaml.extended.infrastrucutre;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("AwsVm")
@TypeAlias("AwsVmInfraYaml")
@OwnedBy(CI)
public class AwsVmInfraYaml implements Infrastructure {
  @Builder.Default @NotNull private Type type = Type.AWS_VM;
  @NotNull private AwsVmInfraYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AwsVmInfraYamlSpec {
    @NotNull private String poolId;
  }
}
