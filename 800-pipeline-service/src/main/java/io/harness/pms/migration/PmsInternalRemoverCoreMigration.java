package io.harness.pms.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.pms.sdk.PmsSdkInstance;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class PmsInternalRemoverCoreMigration implements NGMigration {
  @Inject private final MongoTemplate mongoTemplate;

  @Override
  public void migrate() {
    Criteria criteria = Criteria.where(NgPipelineEntity.PipelineNGKeys.name).is("pmsInternal");
    Query query = new Query(criteria);
    mongoTemplate.remove(query, PmsSdkInstance.class);
  }
}
