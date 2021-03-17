package io.harness.steps.barriers.service.visitor;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.beans.StageDetail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BarrierVisitorTest extends OrchestrationStepsTestBase {
  @Inject private Injector injector;
  private BarrierVisitor barrierVisitor;

  @Before
  public void setUp() {
    barrierVisitor = new BarrierVisitor(injector);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestMapPopulationFromFlowControlField() throws IOException {
    // this is not a valid Harness yaml, it is used only for the purpose of testing the specific logic
    String yaml = "pipeline:\n"
        + "  name: Test-2\n"
        + "  identifier: Test-2\n"
        + "  flowControl:\n"
        + "    barriers:\n"
        + "      - identifier: myBarrierId1\n"
        + "        name: myBarrier1Name\n"
        + "      - identifier: myBarrierId2\n"
        + "        name: myBarrier2Name\n"
        + "    resourceConstraints:\n"
        + "      - identifier: rcId1\n"
        + "        name: rc1";

    Map<String, BarrierSetupInfo> expectedMap = ImmutableMap.of("myBarrierId1",
        BarrierSetupInfo.builder().identifier("myBarrierId1").name("myBarrier1Name").stages(new HashSet<>()).build(),
        "myBarrierId2",
        BarrierSetupInfo.builder().identifier("myBarrierId2").name("myBarrier2Name").stages(new HashSet<>()).build());
    YamlNode yamlNode = YamlUtils.extractPipelineField(yaml).getNode();
    barrierVisitor.walkElementTree(yamlNode);

    assertThat(barrierVisitor.getBarrierIdentifierMap()).isNotEmpty();
    assertThat(barrierVisitor.getBarrierIdentifierMap().size()).isEqualTo(2);
    assertThat(barrierVisitor.getBarrierIdentifierMap()).containsAllEntriesOf(expectedMap);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestBarrierVisitorSuccess() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String yamlFile = "barriers.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(yamlFile)), StandardCharsets.UTF_8);

    YamlNode yamlNode = YamlUtils.extractPipelineField(yaml).getNode();
    barrierVisitor.walkElementTree(yamlNode);

    String vbStage = "VBStage";
    String sampleStage = "SampleStage";
    Map<String, BarrierSetupInfo> expectedMap = ImmutableMap.of("myBarrierId1",
        BarrierSetupInfo.builder()
            .identifier("myBarrierId1")
            .name("myBarrier1Name")
            .stages(ImmutableSet.of(StageDetail.builder().identifier(vbStage).name(vbStage).build(),
                StageDetail.builder().identifier(sampleStage).name(sampleStage).build()))
            .timeout(600_000)
            .build(),
        "myBarrierId2",
        BarrierSetupInfo.builder()
            .identifier("myBarrierId2")
            .name("myBarrier2Name")
            .stages(ImmutableSet.of(StageDetail.builder().identifier(vbStage).name(vbStage).build()))
            .timeout(1_200_000)
            .build(),
        "myBarrierId3",
        BarrierSetupInfo.builder()
            .identifier("myBarrierId3")
            .name("myBarrier3Name")
            .stages(new HashSet<>())
            .timeout(0)
            .build());

    assertThat(barrierVisitor.getBarrierIdentifierMap()).isNotEmpty();
    assertThat(barrierVisitor.getBarrierIdentifierMap().size()).isEqualTo(3);
    assertThat(barrierVisitor.getBarrierIdentifierMap()).containsAllEntriesOf(expectedMap);
  }
}
