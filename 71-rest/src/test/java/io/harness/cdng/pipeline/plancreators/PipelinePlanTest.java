package io.harness.cdng.pipeline.plancreators;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.executionplan.ExecutionPlanCreatorRegistrar;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class PipelinePlanTest extends WingsBaseTest {
  @Inject ExecutionPlanCreatorRegistrar executionPlanCreatorRegistrar;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  @Before
  public void setUp() throws Exception {
    executionPlanCreatorRegistrar.register();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testPipelinePlanForGivenYaml() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/multiStagePipeline.yml");
    CDPipeline cdPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);
    final Plan planForPipeline =
        executionPlanCreatorService.createPlanForPipeline(cdPipeline, WingsTestConstants.ACCOUNT_ID);
    List<PlanNode> planNodes = planForPipeline.getNodes();
    List<PlanNode> pipelinePlanNodeList = planNodes.stream()
                                              .filter(p -> p.getIdentifier().equals("managerServiceDeployment"))
                                              .collect(Collectors.toList());
    assertThat(pipelinePlanNodeList.size()).isEqualTo(1);

    List<PlanNode> qaStageList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("qaStage")).collect(Collectors.toList());
    assertThat(qaStageList.size()).isEqualTo(1);
    List<PlanNode> prodStageNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("prodStage")).collect(Collectors.toList());
    assertThat(prodStageNodesList.size()).isEqualTo(1);

    List<PlanNode> serviceNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("service")).collect(Collectors.toList());
    assertThat(serviceNodesList.size()).isEqualTo(2);
    List<PlanNode> artifactsNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("artifacts")).collect(Collectors.toList());
    assertThat(artifactsNodesList.size()).isEqualTo(2);
    List<PlanNode> primaryArtifactNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("primary")).collect(Collectors.toList());
    assertThat(primaryArtifactNodesList.size()).isEqualTo(2);
    List<PlanNode> infrastructureNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("infrastructure")).collect(Collectors.toList());
    assertThat(infrastructureNodesList.size()).isEqualTo(2);
    List<PlanNode> manifestsNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("MANIFESTS")).collect(Collectors.toList());
    assertThat(manifestsNodesList.size()).isEqualTo(2);

    List<PlanNode> executionNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("EXECUTION")).collect(Collectors.toList());
    assertThat(executionNodesList.size()).isEqualTo(2);

    List<PlanNode> stage1RollOutNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("rolloutDeployment1")).collect(Collectors.toList());
    assertThat(stage1RollOutNodesList.size()).isEqualTo(1);
    List<PlanNode> stage2RollOutNodesList =
        planNodes.stream().filter(p -> p.getIdentifier().equals("rolloutDeployment2")).collect(Collectors.toList());
    assertThat(stage2RollOutNodesList.size()).isEqualTo(1);
    List<PlanNode> stage2RollBackNodesList = planNodes.stream()
                                                 .filter(p -> p.getIdentifier().equals("rollbackRolloutDeployment2"))
                                                 .collect(Collectors.toList());
    assertThat(stage2RollBackNodesList.size()).isEqualTo(1);
    List<PlanNode> stage1RollBackNodesList = planNodes.stream()
                                                 .filter(p -> p.getIdentifier().equals("rollbackRolloutDeployment1"))
                                                 .collect(Collectors.toList());
    assertThat(stage1RollBackNodesList.size()).isEqualTo(1);
  }
}
