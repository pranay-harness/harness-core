package io.harness.ng.pipeline.resources;

import static io.harness.rule.OwnerRule.SANYASI_NAIDU;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineRequestDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.cdng.pipeline.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.service.PipelineServiceImpl;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.io.harness.ng.utils.PageTestUtils;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CDNGPipelineResourceTest extends CategoryTest {
  @Mock PipelineServiceImpl pipelineService;
  CDNGPipelineResource cdngPipelineResource;
  @Mock NgPipelineExecutionService ngPipelineExecutionService;
  @Mock RestQueryFilterParser restQueryFilterParser;
  CDPipelineResponseDTO cdPipelineResponseDTO;
  CDPipelineRequestDTO cdPipelineRequestDTO;
  CDPipelineEntity cdPipelineEntity;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("pipeline.yaml").getFile());
    CDPipeline cdPipeline = YamlPipelineUtils.read(file.toURL(), CDPipeline.class);
    cdngPipelineResource = new CDNGPipelineResource(pipelineService, restQueryFilterParser, ngPipelineExecutionService);
    cdPipelineRequestDTO = cdPipelineRequestDTO.builder().cdPipeline(cdPipeline).build();
    cdPipelineResponseDTO =
        cdPipelineResponseDTO.builder().cdPipeline(cdPipeline).executionsPlaceHolder(new ArrayList<>()).build();
    cdPipelineEntity = cdPipelineEntity.builder()
                           .accountId("ACCOUNT_ID")
                           .projectIdentifier("PROJECT_ID")
                           .orgIdentifier("ORG_ID")
                           .identifier("managerServiceDeployment")
                           .cdPipeline(cdPipeline)
                           .build();
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testGetPipeline() throws IOException {
    doReturn(Optional.of(cdPipelineResponseDTO))
        .when(pipelineService)
        .getPipeline(cdPipelineRequestDTO.getCdPipeline().getIdentifier(), "ACCOUNT_ID", "ORG_ID", "PROJECT_ID");
    Optional<CDPipelineResponseDTO> pipelineResponse =
        cdngPipelineResource.getPipelineByIdentifier("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "managerServiceDeployment")
            .getData();
    assertThat(pipelineResponse).isPresent();
    assertThat(pipelineResponse.get()).isEqualTo(cdPipelineResponseDTO);
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testCreatePipeline() {
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("pipeline.yaml").getFile());
    doReturn(cdPipelineEntity.getIdentifier())
        .when(pipelineService)
        .createPipeline(Files.contentOf(file, Charset.defaultCharset()), "ACCOUNT_ID", "ORG_ID", "PROJECT_ID");
    String yamlIdentifierActual =
        cdngPipelineResource
            .createPipeline("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", Files.contentOf(file, Charset.defaultCharset()))
            .getData();
    assertThat(yamlIdentifierActual).isEqualTo(cdPipelineResponseDTO.getCdPipeline().getIdentifier());
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testListPipelines() {
    List<CDPipelineResponseDTO> pipelineEntityList = new ArrayList<>();
    when(pipelineService.getPipelines(anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class)))
        .thenReturn(PageTestUtils.getPage(pipelineEntityList, 0));
    assertTrue(cdngPipelineResource.getListOfPipelines("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "", 10, 10, null)
                   .getData()
                   .isEmpty());

    Pageable pageable = PageUtils.getPageRequest(0, 10, null);
    final Page<CDPipelineResponseDTO> pipelineEntities =
        new PageImpl<>(Collections.singletonList(cdPipelineResponseDTO), pageable, 1);
    doReturn(pipelineEntities)
        .when(pipelineService)
        .getPipelines(anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class));

    List<CDPipelineResponseDTO> content =
        cdngPipelineResource.getListOfPipelines("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "", 10, 10, null)
            .getData()
            .getContent();
    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(cdPipelineResponseDTO);
  }
}
