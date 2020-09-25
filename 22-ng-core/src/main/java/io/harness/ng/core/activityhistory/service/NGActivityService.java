package io.harness.ng.core.activityhistory.service;

import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import org.springframework.data.domain.Page;

public interface NGActivityService {
  Page<NGActivityDTO> list(int page, int size, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String referredEntityIdentifier);

  NGActivityDTO save(NGActivityDTO activityHistory);
}
