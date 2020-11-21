package io.harness.app.intfc;

import io.harness.app.beans.dto.CIBuildFilterDTO;
import io.harness.app.beans.dto.CIBuildResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CIBuildInfoService {
  CIBuildResponseDTO getBuild(Long buildId, String accountId, String orgId, String projectId);
  Page<CIBuildResponseDTO> getBuilds(CIBuildFilterDTO ciBuildFilterDTO, Pageable pageable);
}
