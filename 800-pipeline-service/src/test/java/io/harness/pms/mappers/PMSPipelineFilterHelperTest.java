package io.harness.pms.mappers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.filters.PMSPipelineFilterRequestDTO;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public class PMSPipelineFilterHelperTest extends PipelineServiceTestBase {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_ID = "ordId";
  private static final String PROJECT_ID = "projectId";

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetUpdateOperations() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().build();
    Update update = new Update();
    update.set(PipelineEntity.PipelineEntityKeys.accountId, pipelineEntity.getAccountId());
    update.set(PipelineEntity.PipelineEntityKeys.orgIdentifier, pipelineEntity.getOrgIdentifier());
    update.set(PipelineEntity.PipelineEntityKeys.projectIdentifier, pipelineEntity.getProjectIdentifier());
    update.set(PipelineEntity.PipelineEntityKeys.identifier, pipelineEntity.getIdentifier());
    update.set(PipelineEntity.PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    update.set(PipelineEntity.PipelineEntityKeys.tags, pipelineEntity.getTags());
    update.set(PipelineEntity.PipelineEntityKeys.deleted, false);
    update.set(PipelineEntity.PipelineEntityKeys.description, pipelineEntity.getDescription());

    assertThat(update).isEqualTo(PMSPipelineFilterHelper.getUpdateOperations(pipelineEntity));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCreateCriteriaForGetList() {
    Map<String, List<String>> filters = new HashMap<>();
    filters.put("filters.cd.deploymentTypes", Collections.singletonList("deploymentTypes"));
    PMSPipelineFilterRequestDTO pmsPipelineFilterRequestDTO =
        PMSPipelineFilterRequestDTO.builder().filters(filters).build();
    Criteria returnedCriteria = PMSPipelineFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, PROJECT_ID, ORG_ID, pmsPipelineFilterRequestDTO, "cd", null, false);
    Criteria criteria = new Criteria();
    criteria.and(PipelineEntity.PipelineEntityKeys.accountId)
        .is(ACCOUNT_ID)
        .and(PipelineEntity.PipelineEntityKeys.orgIdentifier)
        .is(ORG_ID)
        .and(PipelineEntity.PipelineEntityKeys.projectIdentifier)
        .is(PROJECT_ID)
        .and(PipelineEntity.PipelineEntityKeys.deleted)
        .is(false)
        .and("filters.cd.deploymentTypes")
        .in(filters.get("filters.cd.deploymentTypes"))
        .and("filters.cd")
        .exists(true);

    assertThat(returnedCriteria.toString()).isEqualTo(criteria.toString());
  }
}