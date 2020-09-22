package io.harness.ngpipeline.overlayinputset.services;

import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface OverlayInputSetEntityService {
  OverlayInputSetEntity create(OverlayInputSetEntity cdInputSetEntity);

  Optional<OverlayInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted);

  OverlayInputSetEntity update(OverlayInputSetEntity requestOverlayInputSet);

  OverlayInputSetEntity upsert(OverlayInputSetEntity requestOverlayInputSet);

  Page<OverlayInputSetEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier);
}
