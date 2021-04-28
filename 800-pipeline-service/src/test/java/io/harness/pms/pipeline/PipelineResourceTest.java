package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.NodeExecutionToExecutioNodeMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@OwnedBy(PIPELINE)
public class PipelineResourceTest extends CategoryTest {
  PipelineResource pipelineResource;
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock PMSYamlSchemaService pmsYamlSchemaService;
  @Mock NodeExecutionService nodeExecutionService;
  @Mock AccessControlClient accessControlClient;
  @Mock NodeExecutionToExecutioNodeMapper nodeExecutionToExecutioNodeMapper;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "basichttpFail";
  private String yaml;

  PipelineEntity entity;
  PipelineEntity entityWithVersion;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    pipelineResource = new PipelineResource(pmsPipelineService, pmsExecutionService, pmsYamlSchemaService,
        nodeExecutionService, accessControlClient, nodeExecutionToExecutioNodeMapper);
    ClassLoader classLoader = this.getClass().getClassLoader();
    String filename = "failure-strategy.yaml";
    yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    entity = PipelineEntity.builder()
                 .accountId(ACCOUNT_ID)
                 .orgIdentifier(ORG_IDENTIFIER)
                 .projectIdentifier(PROJ_IDENTIFIER)
                 .identifier(PIPELINE_IDENTIFIER)
                 .name(PIPELINE_IDENTIFIER)
                 .yaml(yaml)
                 .build();

    entityWithVersion = PipelineEntity.builder()
                            .accountId(ACCOUNT_ID)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJ_IDENTIFIER)
                            .identifier(PIPELINE_IDENTIFIER)
                            .name(PIPELINE_IDENTIFIER)
                            .yaml(yaml)
                            .stageCount(1)
                            .stageName("qaStage")
                            .version(1L)
                            .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreatePipeline() {
    doReturn(entityWithVersion).when(pmsPipelineService).create(entity);
    ResponseDTO<String> identifier =
        pipelineResource.createPipeline(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, yaml);
    assertThat(identifier.getData()).isNotEmpty();
    assertThat(identifier.getData()).isEqualTo(PIPELINE_IDENTIFIER);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetPipeline() {
    doReturn(Optional.of(entityWithVersion))
        .when(pmsPipelineService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    ResponseDTO<PMSPipelineResponseDTO> responseDTO = pipelineResource.getPipelineByIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null);
    assertThat(responseDTO.getData().getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getData().getYamlPipeline()).isEqualTo(yaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdatePipelineWithWrongIdentifier() {
    String incorrectPipelineIdentifier = "notTheIdentifierWeNeed";
    assertThatThrownBy(()
                           -> pipelineResource.updatePipeline(null, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
                               incorrectPipelineIdentifier, null, yaml))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Pipeline identifier in URL does not match pipeline identifier in yaml");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdatePipeline() {
    doReturn(entityWithVersion).when(pmsPipelineService).update(entity);
    ResponseDTO<String> responseDTO = pipelineResource.updatePipeline(
        null, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null, yaml);
    assertThat(responseDTO.getData()).isEqualTo(PIPELINE_IDENTIFIER);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDeletePipeline() throws ProducerShutdownException {
    doReturn(true)
        .when(pmsPipelineService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null);
    ResponseDTO<Boolean> deleteResponse =
        pipelineResource.deletePipeline(null, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null);
    assertThat(deleteResponse.getData()).isEqualTo(true);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetPipelineSummary() {
    doReturn(Optional.of(entityWithVersion))
        .when(pmsPipelineService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    ResponseDTO<PMSPipelineSummaryResponseDTO> pipelineSummary =
        pipelineResource.getPipelineSummary(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER);
    assertThat(pipelineSummary.getData().getName()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(pipelineSummary.getData().getIdentifier()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(pipelineSummary.getData().getDescription()).isNull();
    assertThat(pipelineSummary.getData().getTags()).isEmpty();
    assertThat(pipelineSummary.getData().getVersion()).isEqualTo(1L);
    assertThat(pipelineSummary.getData().getNumOfStages()).isEqualTo(1L);
    assertThat(pipelineSummary.getData().getStageNames().get(0)).isEqualTo("qaStage");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetListOfPipelines() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));
    Page<PipelineEntity> pipelineEntities = new PageImpl<>(Collections.singletonList(entityWithVersion), pageable, 1);
    doReturn(pipelineEntities).when(pmsPipelineService).list(any(), any());
    List<PMSPipelineSummaryResponseDTO> content =
        pipelineResource
            .getListOfPipelines(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, 0, 25, null, null, null, null, null, null)
            .getData()
            .getContent();
    assertThat(content).isNotEmpty();
    assertThat(content.size()).isEqualTo(1);

    PMSPipelineSummaryResponseDTO responseDTO = content.get(0);
    assertThat(responseDTO.getIdentifier()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(responseDTO.getName()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(responseDTO.getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getNumOfStages()).isEqualTo(1L);
    assertThat(responseDTO.getStageNames().get(0)).isEqualTo("qaStage");
  }
}