package io.harness.integrationstage;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.yaml.extended.infrastrucutre.AwsVmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.AwsVmInfraYaml.AwsVmInfraYamlSpec;
import io.harness.plancreator.stages.stage.StageElementConfig;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(CI)
public class AwsVmBuildJobTestHelper {
  public StageElementConfig getAwsVmStage(String poolId) {
    AwsVmInfraYaml awsVmInfraYaml =
        AwsVmInfraYaml.builder().spec(AwsVmInfraYamlSpec.builder().poolId(poolId).build()).build();
    StageElementConfig stageElementConfig =
        StageElementConfig.builder()
            .stageType(IntegrationStageConfig.builder().infrastructure(awsVmInfraYaml).build())
            .build();
    return stageElementConfig;
  }
}
