package io.harness.repositories.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PMSInputSetRepositoryCustom {
  InputSetEntity update(Criteria criteria, InputSetEntity inputSetEntity);

  UpdateResult delete(Criteria criteria);

  UpdateResult deleteAllInputSetsWhenPipelineDeleted(Query query, Update update);

  Page<InputSetEntity> findAll(Criteria criteria, Pageable pageable);
}
