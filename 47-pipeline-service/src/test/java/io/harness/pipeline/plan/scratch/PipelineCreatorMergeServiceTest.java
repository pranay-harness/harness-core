package io.harness.pipeline.plan.scratch;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pipeline.plan.scratch.cd.CDPlanCreatorService;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreationResponse;
import io.harness.pipeline.plan.scratch.cv.CVPlanCreatorService;
import io.harness.pipeline.plan.scratch.pms.creator.PlanCreatorMergeService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PipelineCreatorMergeServiceTest extends PipelineServiceTestBase {
  private String pipelineContent = null;

  @Before
  public void setup() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream pipelineStream = classLoader.getResourceAsStream("pipeline.yml");
    assertThat(pipelineStream).isNotNull();
    StringBuilder textBuilder = new StringBuilder();
    try (Reader reader = new BufferedReader(
             new InputStreamReader(pipelineStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    }
    pipelineContent = textBuilder.toString();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCreatePlan() throws IOException {
    PlanCreatorMergeService planCreatorMergeService =
        new PlanCreatorMergeService(Lists.newArrayList(new CDPlanCreatorService(), new CVPlanCreatorService()));

    PlanCreationResponse planCreationResponse = planCreatorMergeService.createPlan(pipelineContent);
    assertThat(planCreationResponse).isNotNull();
    assertThat(planCreationResponse.getNodes().size()).isEqualTo(9);
    assertThat(planCreationResponse.getDependencies().size()).isEqualTo(0);
    assertThat(planCreationResponse.getStartingNodeId()).isNotNull();
    assertThat(planCreationResponse.getNodes()).containsKey(planCreationResponse.getStartingNodeId());
  }
}
