package io.harness.app.intfc;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface CIBuildInfoService {
  CIBuildResponseDTO getBuild(Long buildId, String accountId, String orgId, String projectId);
  Page<CIBuildResponseDTO> getBuilds(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable);
}