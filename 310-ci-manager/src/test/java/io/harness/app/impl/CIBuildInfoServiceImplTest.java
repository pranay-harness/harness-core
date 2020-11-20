package io.harness.app.impl;

import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.ACCOUNT_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.BUILD_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.ORG_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.PIPELINE_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.PROJECT_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getBasicBuild;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getBasicBuildDTO;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getBuildFilter;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getPipeline;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.app.beans.dto.CIBuildFilterDTO;
import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.dao.repositories.CIBuildInfoRepository;
import io.harness.app.mappers.BuildDtoMapper;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;
import javax.ws.rs.NotFoundException;

public class CIBuildInfoServiceImplTest extends CIManagerTest {
  @Mock private CIBuildInfoRepository ciBuildInfoRepository;
  @Mock private BuildDtoMapper buildDtoMapper;
  @Mock private NGPipelineService ngPipelineService;
  @InjectMocks CIBuildInfoServiceImpl ciBuildInfoService;

  @Test(expected = NotFoundException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBuildByIDNotFoundError() {
    when(ciBuildInfoRepository.getBuildById(ACCOUNT_ID, ORG_ID, PROJECT_ID, BUILD_ID))
        .thenReturn(Optional.ofNullable(null));

    ciBuildInfoService.getBuild(BUILD_ID, ACCOUNT_ID, ORG_ID, PROJECT_ID);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBuildByID() {
    CIBuild ciBuild = getBasicBuild();
    NgPipelineEntity ngPipelineEntity = getPipeline();
    CIBuildResponseDTO ciBuildResponseDTO = getBasicBuildDTO();
    when(ciBuildInfoRepository.getBuildById(ACCOUNT_ID, ORG_ID, PROJECT_ID, BUILD_ID))
        .thenReturn(Optional.ofNullable(ciBuild));
    when(ngPipelineService.getPipeline(PIPELINE_ID, ACCOUNT_ID, ORG_ID, PROJECT_ID)).thenReturn(ngPipelineEntity);
    when(buildDtoMapper.writeBuildDto(ciBuild, ngPipelineEntity)).thenReturn(ciBuildResponseDTO);
    CIBuildResponseDTO responseDTO = ciBuildInfoService.getBuild(BUILD_ID, ACCOUNT_ID, ORG_ID, PROJECT_ID);
    assertEquals(responseDTO.getId(), BUILD_ID);
    assertEquals(responseDTO.getPipeline().getId(), PIPELINE_ID);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBuilds() {
    CIBuildFilterDTO ciBuildFilterDTO = CIBuildFilterDTO.builder().build();
    Pageable pageable = PageRequest.of(1, 5);
    when(ngPipelineService.listPipelines(any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl(Arrays.asList(getPipeline())));
    when(ciBuildInfoRepository.getBuilds(any(), any())).thenReturn(Page.empty());
    Page<CIBuildResponseDTO> responseDTO = ciBuildInfoService.getBuilds(ciBuildFilterDTO, pageable);
    assertEquals(responseDTO.getTotalElements(), 0);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBuildsWithFilters() {
    CIBuildFilterDTO ciBuildFilterDTO = getBuildFilter();
    Pageable pageable = PageRequest.of(1, 5);
    when(ngPipelineService.listPipelines(any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl(Arrays.asList(getPipeline())));
    when(ciBuildInfoRepository.getBuilds(any(), any())).thenReturn(Page.empty());
    Page<CIBuildResponseDTO> responseDTO = ciBuildInfoService.getBuilds(ciBuildFilterDTO, pageable);
    assertEquals(responseDTO.getTotalElements(), 0);
  }
}
