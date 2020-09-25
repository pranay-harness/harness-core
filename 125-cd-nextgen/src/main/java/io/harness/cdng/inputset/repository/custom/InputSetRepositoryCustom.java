package io.harness.cdng.inputset.repository.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.ngpipeline.BaseInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface InputSetRepositoryCustom {
  Page<BaseInputSetEntity> findAll(Criteria criteria, Pageable pageable);
  UpdateResult update(Criteria criteria, BaseInputSetEntity baseInputSetEntity);
  UpdateResult delete(Criteria criteria);
}
