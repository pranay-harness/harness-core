package io.harness.pms.mappers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PMSPipelineFilterHelperTest extends PipelineServiceTestBase {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().build();
    List<String> fieldsToBeUpdated = new ArrayList<>();
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.name);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.accountId);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.orgIdentifier);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.projectIdentifier);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.yaml);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.tags);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.deleted);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.description);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.stageCount);
    fieldsToBeUpdated.add(PipelineEntity.PipelineEntityKeys.lastUpdatedAt);

    for (String field : fieldsToBeUpdated) {
      assertThat(true).isEqualTo(PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity).modifies(field));
    }
  }
}