package io.harness.integrationstage;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmPoolYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmPoolYaml.VmPoolYamlSpec;
import io.harness.plancreator.stages.stage.StageElementConfig;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(CI)
public class VmBuildJobTestHelper {
  public StageElementConfig getVmStage(String poolId) {
    VmInfraYaml awsVmInfraYaml =
        VmInfraYaml.builder()
            .spec(VmPoolYaml.builder().spec(VmPoolYamlSpec.builder().identifier(poolId).build()).build())
            .build();
    StageElementConfig stageElementConfig =
        StageElementConfig.builder()
            .type("CI")
            .stageType(IntegrationStageConfig.builder().infrastructure(awsVmInfraYaml).build())
            .build();
    return stageElementConfig;
  }
}
