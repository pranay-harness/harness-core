package io.harness.ng.pipeline.resources;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.SANYASI_NAIDU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.fabric8.utils.Lists;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStrategyType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineRequestDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.service.PipelineServiceImpl;
import io.harness.cdng.service.beans.ServiceDefinitionType;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CDNGPipelineResourceTest extends CategoryTest {
  @Mock PipelineServiceImpl pipelineService;
  CDNGPipelineResource cdngPipelineResource;
  @Mock NgPipelineExecutionService ngPipelineExecutionService;
  @Mock RestQueryFilterParser restQueryFilterParser;
  CDPipelineResponseDTO cdPipelineResponseDTO;
  CDPipelineRequestDTO cdPipelineRequestDTO;
  CDPipelineEntity cdPipelineEntity;
  CDPipelineSummaryResponseDTO cdPipelineSummaryResponseDTO;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("pipeline.yaml").getFile());
    CDPipeline cdPipeline = YamlPipelineUtils.read(file.toURL(), CDPipeline.class);
    cdngPipelineResource = new CDNGPipelineResource(pipelineService, restQueryFilterParser, ngPipelineExecutionService);
    cdPipelineRequestDTO = CDPipelineRequestDTO.builder().cdPipeline(cdPipeline).build();
    cdPipelineResponseDTO =
        CDPipelineResponseDTO.builder().cdPipeline(cdPipeline).executionsPlaceHolder(new ArrayList<>()).build();
    cdPipelineEntity = CDPipelineEntity.builder()
                           .accountId(ACCOUNT_ID)
                           .projectIdentifier(PROJ_IDENTIFIER)
                           .orgIdentifier(ORG_IDENTIFIER)
                           .identifier("managerServiceDeployment")
                           .cdPipeline(cdPipeline)
                           .build();
    cdPipelineSummaryResponseDTO = CDPipelineSummaryResponseDTO.builder()
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
    doReturn(Optional.of(cdPipelineResponseDTO))
        .when(pipelineService)
        .getPipeline(cdPipelineRequestDTO.getCdPipeline().getIdentifier(), ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER);
    CDPipelineResponseDTO pipelineResponse =
        cdngPipelineResource
            .getPipelineByIdentifier(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "managerServiceDeployment")
            .getData();
    assertThat(pipelineResponse).isNotNull();
    assertThat(pipelineResponse).isEqualTo(cdPipelineResponseDTO);
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testCreatePipeline() {
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("pipeline.yaml").getFile());
    doReturn(cdPipelineEntity.getIdentifier())
        .when(pipelineService)
        .createPipeline(Files.contentOf(file, Charset.defaultCharset()), ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER);
    String yamlIdentifierActual = cdngPipelineResource
                                      .createPipeline(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
                                          Files.contentOf(file, Charset.defaultCharset()))
                                      .getData();
    assertThat(yamlIdentifierActual).isEqualTo(cdPipelineResponseDTO.getCdPipeline().getIdentifier());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetListOfPipelines() {
    List<CDPipelineSummaryResponseDTO> emptySummaryResponseList = new ArrayList<>();
    doReturn(PageTestUtils.getPage(emptySummaryResponseList, 0))
        .when(pipelineService)
        .getPipelines(anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class), anyString());
    assertThat(
        cdngPipelineResource.getListOfPipelines(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "", 10, 10, null, "")
            .getData()
            .isEmpty())
        .isTrue();

    Pageable pageable = PageUtils.getPageRequest(0, 10, null);
    Page<CDPipelineSummaryResponseDTO> summaryResponseDTOs =
        new PageImpl<>(Collections.singletonList(cdPipelineSummaryResponseDTO), pageable, 1);
    doReturn(summaryResponseDTOs)
        .when(pipelineService)
        .getPipelines(anyString(), anyString(), anyString(), any(Criteria.class), any(Pageable.class), anyString());

    List<CDPipelineSummaryResponseDTO> content =
        cdngPipelineResource.getListOfPipelines(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "", 10, 10, null, "")
            .getData()
            .getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(cdPipelineSummaryResponseDTO);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionStrategyList() throws IOException {
    doCallRealMethod().when(pipelineService).getExecutionStrategyList();
    Map<ServiceDefinitionType, List<ExecutionStrategyType>> executionStrategyResponse =
        cdngPipelineResource.getExecutionStrategyList().getData();

    assertThat(executionStrategyResponse).isNotNull();
    assertThat(executionStrategyResponse.keySet().size()).isEqualTo(5);
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.KUBERNETES))
        .isEqualTo(Lists.newArrayList(
            ExecutionStrategyType.ROLLING, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.HELM))
        .isEqualTo(Lists.newArrayList(ExecutionStrategyType.BASIC));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.PCF))
        .isEqualTo(Lists.newArrayList(
            ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.SSH))
        .isEqualTo(Lists.newArrayList(ExecutionStrategyType.BASIC));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.ECS))
        .isEqualTo(Lists.newArrayList(
            ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetServiceDefinitionTypes() {
    when(pipelineService.getServiceDefinitionTypes()).thenReturn(Arrays.asList(ServiceDefinitionType.values()));
    List<ServiceDefinitionType> serviceDefinitionTypes = cdngPipelineResource.getServiceDefinitionTypes().getData();

    assertThat(serviceDefinitionTypes).isNotNull();
    assertThat(serviceDefinitionTypes.size()).isEqualTo(5);
  }
}
