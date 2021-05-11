package io.harness.repositories.inputset.custom;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@ToBeDeleted
@Deprecated
public interface InputSetRepositoryCustom {
  Page<BaseInputSetEntity> findAll(Criteria criteria, Pageable pageable);
  BaseInputSetEntity update(Criteria criteria, BaseInputSetEntity baseInputSetEntity);
  UpdateResult delete(Criteria criteria);
}
