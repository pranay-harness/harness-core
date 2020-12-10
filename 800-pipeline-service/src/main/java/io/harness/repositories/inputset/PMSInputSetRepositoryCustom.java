package io.harness.repositories.inputset;

import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PMSInputSetRepositoryCustom {
  InputSetEntity update(Criteria criteria, InputSetEntity inputSetEntity);

  UpdateResult delete(Criteria criteria);

  Page<InputSetEntity> findAll(Criteria criteria, Pageable pageable);
}
