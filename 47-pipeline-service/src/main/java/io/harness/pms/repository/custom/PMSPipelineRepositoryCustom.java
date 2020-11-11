package io.harness.pms.repository.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.pms.beans.entities.PipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PMSPipelineRepositoryCustom {
  PipelineEntity update(Criteria criteria, PipelineEntity pipelineEntity);

  UpdateResult delete(Criteria criteria);

  Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable);
}
