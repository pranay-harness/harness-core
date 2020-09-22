package io.harness.ngpipeline.overlayinputset.repository.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface OverlayInputSetRepositoryCustom {
  Page<OverlayInputSetEntity> findAll(Criteria criteria, Pageable pageable);
  UpdateResult upsert(Criteria criteria, OverlayInputSetEntity overlayInputSetEntity);
  UpdateResult update(Criteria criteria, OverlayInputSetEntity overlayInputSetEntity);
  UpdateResult delete(Criteria criteria);
}
