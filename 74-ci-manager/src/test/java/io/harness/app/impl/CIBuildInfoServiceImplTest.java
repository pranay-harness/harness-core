package io.harness.app.impl;

import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.ACCOUNT_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.BUILD_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.ORG_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.PIPELINE_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.PROJECT_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getBasicBuild;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getBasicBuildDTO;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.dao.repositories.CIBuildInfoRepository;
import io.harness.app.mappers.BuildDtoMapper;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;
import javax.ws.rs.NotFoundException;

public class CIBuildInfoServiceImplTest extends CIManagerTest {
  @Mock private CIBuildInfoRepository ciBuildInfoRepository;
  @Mock private BuildDtoMapper buildDtoMapper;
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
    CIBuildResponseDTO ciBuildResponseDTO = getBasicBuildDTO();
    when(ciBuildInfoRepository.getBuildById(ACCOUNT_ID, ORG_ID, PROJECT_ID, BUILD_ID))
        .thenReturn(Optional.ofNullable(ciBuild));
    when(buildDtoMapper.writeBuildDto(ciBuild, ACCOUNT_ID, ORG_ID, PROJECT_ID)).thenReturn(ciBuildResponseDTO);
    CIBuildResponseDTO responseDTO = ciBuildInfoService.getBuild(BUILD_ID, ACCOUNT_ID, ORG_ID, PROJECT_ID);
    assertEquals(responseDTO.getId(), BUILD_ID);
    assertEquals(responseDTO.getPipeline().getId(), PIPELINE_ID);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBuilds() {
    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(1, 5);
    when(ciBuildInfoRepository.getBuilds(eq(ACCOUNT_ID), eq(ORG_ID), eq(PROJECT_ID), any(), any()))
        .thenReturn(Page.empty());
    Page<CIBuildResponseDTO> responseDTO =
        ciBuildInfoService.getBuilds(ACCOUNT_ID, ORG_ID, PROJECT_ID, criteria, pageable);
    assertEquals(responseDTO.getTotalElements(), 0);
  }
}