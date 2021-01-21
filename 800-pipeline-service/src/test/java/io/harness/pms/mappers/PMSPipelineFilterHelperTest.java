package io.harness.pms.mappers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Update;

public class PMSPipelineFilterHelperTest extends PipelineServiceTestBase {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().build();
    Update update = new Update();
    update.set(PipelineEntity.PipelineEntityKeys.accountId, pipelineEntity.getAccountId());
    update.set(PipelineEntity.PipelineEntityKeys.orgIdentifier, pipelineEntity.getOrgIdentifier());
    update.set(PipelineEntity.PipelineEntityKeys.projectIdentifier, pipelineEntity.getProjectIdentifier());
    update.set(PipelineEntity.PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    update.set(PipelineEntity.PipelineEntityKeys.tags, pipelineEntity.getTags());
    update.set(PipelineEntity.PipelineEntityKeys.deleted, false);
    update.set(PipelineEntity.PipelineEntityKeys.description, pipelineEntity.getDescription());
    update.set(PipelineEntity.PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    update.set(PipelineEntity.PipelineEntityKeys.name, pipelineEntity.getName());

    assertThat(update).isEqualTo(PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity));
  }
}