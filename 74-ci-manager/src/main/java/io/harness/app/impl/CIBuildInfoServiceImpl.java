package io.harness.app.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.beans.dto.CIBuildFilterDTO;
import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.dao.repositories.CIBuildInfoRepository;
import io.harness.app.intfc.CIBuildInfoService;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.mappers.BuildDtoMapper;
import io.harness.beans.CIPipeline;
import io.harness.ci.beans.entities.CIBuild;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.NotFoundException;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CIBuildInfoServiceImpl implements CIBuildInfoService {
  @Inject private CIPipelineService ciPipelineService;
  private final CIBuildInfoRepository ciBuildInfoRepository;
  private final BuildDtoMapper buildDtoMapper;
  private static final String gitIdAttr = "executionSource.user.gitId";
  private static final String branchNameAttr = "executionSource.webhookEvent.branchName";

  public CIBuildResponseDTO getBuild(Long buildId, String accountId, String orgId, String projectId) {
    Optional<CIBuild> ciBuildOptional = ciBuildInfoRepository.getBuildById(accountId, orgId, projectId, buildId);
    if (!ciBuildOptional.isPresent()) {
      throw new NotFoundException(format("Build number:%s not found", buildId));
    }

    CIBuild ciBuild = ciBuildOptional.get();
    CIPipeline ciPipeline =
        ciPipelineService.readPipeline(ciBuild.getPipelineIdentifier(), accountId, orgId, projectId);
    return buildDtoMapper.writeBuildDto(ciBuild, ciPipeline);
  }

  @Override
  public Page<CIBuildResponseDTO> getBuilds(CIBuildFilterDTO ciBuildFilterDTO, Pageable pageable) {
    Criteria criteria = createBuildFilterCriteria(ciBuildFilterDTO);
    Map<String, CIPipeline> ciPipelineMap = getPipelines(ciBuildFilterDTO);
    if (isNotEmpty(ciBuildFilterDTO.getTags())) {
      List<String> pipelineIds = new ArrayList<>();
      pipelineIds.addAll(ciPipelineMap.keySet());
      criteria.and(CIBuild.Build.pipelineIdentifier).in(pipelineIds);
    }

    Page<CIBuild> list = ciBuildInfoRepository.getBuilds(criteria, pageable);
    return list.map(
        ciBuild -> buildDtoMapper.writeBuildDto(ciBuild, ciPipelineMap.get(ciBuild.getPipelineIdentifier())));
  }

  private Criteria createBuildFilterCriteria(CIBuildFilterDTO ciBuildFilterDTO) {
    Criteria criteria = Criteria.where(CIBuild.Build.accountIdentifier)
                            .is(ciBuildFilterDTO.getAccountIdentifier())
                            .and(CIBuild.Build.orgIdentifier)
                            .is(ciBuildFilterDTO.getOrgIdentifier())
                            .and(CIBuild.Build.projectIdentifier)
                            .is(ciBuildFilterDTO.getProjectIdentifier());

    if (isNotBlank(ciBuildFilterDTO.getUserIdentifier())) {
      criteria.and(gitIdAttr).is(ciBuildFilterDTO.getUserIdentifier());
    }
    if (isNotBlank(ciBuildFilterDTO.getBranch())) {
      criteria.and(branchNameAttr).is(ciBuildFilterDTO.getBranch());
    }
    return criteria;
  }

  private Map<String, CIPipeline> getPipelines(CIBuildFilterDTO ciBuildFilterDTO) {
    CIPipelineFilterDTO ciPipelineFilterDTO = CIPipelineFilterDTO.builder()
                                                  .accountIdentifier(ciBuildFilterDTO.getAccountIdentifier())
                                                  .orgIdentifier(ciBuildFilterDTO.getOrgIdentifier())
                                                  .projectIdentifier(ciBuildFilterDTO.getProjectIdentifier())
                                                  .tags(ciBuildFilterDTO.getTags())
                                                  .build();
    Map<String, CIPipeline> ciPipelineMap = new HashMap<>();
    ciPipelineService.getPipelines(ciPipelineFilterDTO)
        .forEach(ciPipeline -> ciPipelineMap.put(ciPipeline.getIdentifier(), ciPipeline));
    return ciPipelineMap;
  }
}
