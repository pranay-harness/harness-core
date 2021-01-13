package io.harness.ng.core.activityhistory.service;

import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckSummaryDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;

import org.springframework.data.domain.Page;

public interface NGActivityService {
  Page<NGActivityDTO> list(int page, int size, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String referredEntityIdentifier, long start, long end, NGActivityStatus status);

  NGActivityDTO save(NGActivityDTO activityHistory);

  boolean deleteAllActivitiesOfAnEntity(String accountIdentifier, String referredEntityFQN);

  ConnectivityCheckSummaryDTO getConnectivityCheckSummary(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long start, long end);
}
