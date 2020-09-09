package io.harness.app.impl;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.dao.repositories.CIBuildInfoRepository;
import io.harness.app.intfc.CIBuildInfoService;
import io.harness.app.mappers.BuildDtoMapper;
import io.harness.ci.beans.entities.CIBuild;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;
import javax.ws.rs.NotFoundException;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CIBuildInfoServiceImpl implements CIBuildInfoService {
  private final CIBuildInfoRepository ciBuildInfoRepository;
  private final BuildDtoMapper buildDtoMapper;

  public CIBuildResponseDTO getBuild(Long buildId, String accountId, String orgId, String projectId) {
    Optional<CIBuild> ciBuild = ciBuildInfoRepository.getBuildById(accountId, orgId, projectId, buildId);
    if (!ciBuild.isPresent()) {
      throw new NotFoundException(format("Build number:%s not found", buildId));
    }
    return buildDtoMapper.writeBuildDto(ciBuild.get(), accountId, orgId, projectId);
  }

  @Override
  public Page<CIBuildResponseDTO> getBuilds(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable) {
    Page<CIBuild> list = ciBuildInfoRepository.getBuilds(accountId, orgId, projectId, criteria, pageable);
    return list.map(ciBuild -> buildDtoMapper.writeBuildDto(ciBuild, accountId, orgId, projectId));
  }
}
