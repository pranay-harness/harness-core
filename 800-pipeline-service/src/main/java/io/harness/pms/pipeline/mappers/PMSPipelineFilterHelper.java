package io.harness.pms.pipeline.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.filter.creation.PMSPipelineFilterRequestDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class PMSPipelineFilterHelper {
  public Update getUpdateOperations(PipelineEntity pipelineEntity) {
    Update update = new Update();
    update.set(PipelineEntityKeys.accountId, pipelineEntity.getAccountId());
    update.set(PipelineEntityKeys.orgIdentifier, pipelineEntity.getOrgIdentifier());
    update.set(PipelineEntityKeys.projectIdentifier, pipelineEntity.getProjectIdentifier());
    update.set(PipelineEntityKeys.identifier, pipelineEntity.getIdentifier());
    update.set(PipelineEntityKeys.yaml, pipelineEntity.getYaml());
    update.set(PipelineEntityKeys.tags, pipelineEntity.getTags());
    update.set(PipelineEntityKeys.deleted, false);
    update.set(PipelineEntityKeys.description, pipelineEntity.getDescription());
    update.set(PipelineEntityKeys.stageCount, pipelineEntity.getStageCount());
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(PipelineEntityKeys.deleted, true);
    return update;
  }

  public Criteria createCriteriaForGetList(String accountId, String orgIdentifier, String projectIdentifier,
      PMSPipelineFilterRequestDTO filter, String module, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(PipelineEntityKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(PipelineEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(PipelineEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.and(PipelineEntityKeys.deleted).is(deleted);

    if (filter != null && EmptyPredicate.isNotEmpty(filter.getFilters())) {
      for (Map.Entry<String, List<String>> filters : filter.getFilters().entrySet()) {
        criteria.and(filters.getKey()).in(filters.getValue());
      }
    }

    if (EmptyPredicate.isNotEmpty(module)) {
      criteria.and(String.format("filters.%s", module)).exists(true);
    }

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria =
          new Criteria().orOperator(where(PipelineEntityKeys.identifier)
                                        .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }

    return criteria;
  }
}
