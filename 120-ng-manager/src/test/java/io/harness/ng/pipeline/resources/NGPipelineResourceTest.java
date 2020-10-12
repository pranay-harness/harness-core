package io.harness.ng.pipeline.resources;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.SANYASI_NAIDU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineRequestDTO;
import io.harness.cdng.pipeline.beans.dto.NGPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.NGPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.cdng.pipeline.service.NGPipelineService;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.rule.Owner;
import io.harness.utils.PageTestUtils;
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

public class NGPipelineResourceTest extends CategoryTest {
  @Mock NGPipelineService ngPipelineService;
  NGPipelineResource ngPipelineResource;
  @Mock RestQueryFilterParser restQueryFilterParser;
  NGPipelineResponseDTO ngPipelineResponseDTO;
  CDPipelineRequestDTO cdPipelineRequestDTO;
  NgPipelineEntity ngPipelineEntity;
  NGPipelineSummaryResponseDTO ngPipelineSummaryResponseDTO;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "k8s";

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("k8sPipeline.yaml").getFile());
    NgPipeline ngPipeline = YamlPipelineUtils.read(file.toURL(), NgPipeline.class);
    ngPipelineResource = new NGPipelineResource(ngPipelineService, restQueryFilterParser);
    cdPipelineRequestDTO = CDPipelineRequestDTO.builder().ngPipeline(ngPipeline).build();
    ngPipelineResponseDTO =
        NGPipelineResponseDTO.builder().ngPipeline(ngPipeline).executionsPlaceHolder(new ArrayList<>()).build();
    ngPipelineEntity = NgPipelineEntity.builder()
                           .accountId(ACCOUNT_ID)
                           .projectIdentifier(PROJ_IDENTIFIER)
                           .orgIdentifier(ORG_IDENTIFIER)
                           .identifier(PIPELINE_IDENTIFIER)
                           .ngPipeline(ngPipeline)
                           .build();
    ngPipelineSummaryResponseDTO = ngPipelineSummaryResponseDTO.builder()
                                       .identifier("pipelineID")
                                       .name("pipelineName")
                                       .numOfStages(0)
                                       .numOfErrors(0)
                                       .deployments(new ArrayList<>())
                                       .build();
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testGetPipeline() throws IOException {
    doReturn(Optional.of(ngPipelineResponseDTO))
        .when(ngPipelineService)
        .getPipelineResponseDTO(
            cdPipelineRequestDTO.getNgPipeline().getIdentifier(), ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER);
    NGPipelineResponseDTO pipelineResponse =
        ngPipelineResource.getPipelineByIdentifier(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER)
            .getData();
    assertThat(pipelineResponse).isNotNull();
    assertThat(pipelineResponse).isEqualTo(ngPipelineResponseDTO);
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testCreatePipeline() {
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("k8sPipeline.yaml").getFile());
    doReturn(ngPipelineEntity.getIdentifier())
        .when(ngPipelineService)
        .createPipeline(Files.contentOf(file, Charset.defaultCharset()), ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER);
    String yamlIdentifierActual = ngPipelineResource
                                      .createPipeline(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
                                          Files.contentOf(file, Charset.defaultCharset()))
                                      .getData();
    assertThat(yamlIdentifierActual).isEqualTo(ngPipelineResponseDTO.getNgPipeline().getIdentifier());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetListOfPipelines() {
    List<NGPipelineSummaryResponseDTO> emptySummaryResponseList = new ArrayList<>();
    doReturn(PageTestUtils.getPage(emptySummaryResponseList, 0))
        .when(ngPipelineService)
        .getPipelinesSummary(
            anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class), anyString());
    assertThat(ngPipelineResource.getListOfPipelines(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "", 10, 10, null, "")
                   .getData()
                   .isEmpty())
        .isTrue();

    Pageable pageable = PageUtils.getPageRequest(0, 10, null);
    Page<NGPipelineSummaryResponseDTO> summaryResponseDTOs =
        new PageImpl<>(Collections.singletonList(ngPipelineSummaryResponseDTO), pageable, 1);
    doReturn(summaryResponseDTOs)
        .when(ngPipelineService)
        .getPipelinesSummary(
            anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class), anyString());

    List<NGPipelineSummaryResponseDTO> content =
        ngPipelineResource.getListOfPipelines(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "", 10, 10, null, "")
            .getData()
            .getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(ngPipelineSummaryResponseDTO);
  }
}
