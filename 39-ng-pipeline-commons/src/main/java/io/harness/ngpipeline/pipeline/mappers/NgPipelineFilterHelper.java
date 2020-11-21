package io.harness.ngpipeline.pipeline.mappers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@UtilityClass
public class NgPipelineFilterHelper {
  public Criteria createCriteriaForGetList(
      String accountId, String orgIdentifier, String projectIdentifier, String searchTerm, boolean deleted) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountId)) {
      criteria.and(PipelineNGKeys.accountId).is(accountId);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(PipelineNGKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(PipelineNGKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.and(PipelineNGKeys.deleted).is(deleted);

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(PipelineNGKeys.identifier).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }

    return criteria;
  }
  public Update getUpdateOperations(NgPipelineEntity ngPipelineEntity) {
    Update update = new Update();
    update.set(PipelineNGKeys.accountId, ngPipelineEntity.getAccountId());
    update.set(PipelineNGKeys.orgIdentifier, ngPipelineEntity.getOrgIdentifier());
    update.set(PipelineNGKeys.projectIdentifier, ngPipelineEntity.getProjectIdentifier());
    update.set(PipelineNGKeys.identifier, ngPipelineEntity.getIdentifier());
    update.set(PipelineNGKeys.ngPipeline, ngPipelineEntity.getNgPipeline());
    update.set(PipelineNGKeys.yamlPipeline, ngPipelineEntity.getYamlPipeline());
    update.set(PipelineNGKeys.tags, ngPipelineEntity.getTags());
    update.set(PipelineNGKeys.referredEntities, ngPipelineEntity.getReferredEntities());
    update.set(PipelineNGKeys.deleted, false);
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(PipelineNGKeys.deleted, true);
    return update;
  }
}
