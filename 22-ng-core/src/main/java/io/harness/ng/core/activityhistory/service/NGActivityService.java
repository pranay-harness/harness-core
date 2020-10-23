package io.harness.ng.core.activityhistory.service;

import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityListDTO;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import org.springframework.data.domain.Page;

public interface NGActivityService {
  NGActivityListDTO list(int page, int size, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String referredEntityIdentifier, long start, long end);

  NGActivityDTO save(NGActivityDTO activityHistory);

  Page<NGActivitySummaryDTO> listActivitySummary(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long start, long end);

  boolean deleteAllActivitiesOfAnEntity(String accountIdentifier, String referredEntityFQN);
}
