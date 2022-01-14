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
@JsonTypeName("Pool")
@TypeAlias("VmPoolYaml")
@OwnedBy(CI)
public class VmPoolYaml implements VmInfraSpec {
  @Builder.Default @NotNull private VmInfraSpec.Type type = VmInfraSpec.Type.POOL;
  @NotNull private VmPoolYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VmPoolYamlSpec {
    @NotNull private String identifier;
  }
}
