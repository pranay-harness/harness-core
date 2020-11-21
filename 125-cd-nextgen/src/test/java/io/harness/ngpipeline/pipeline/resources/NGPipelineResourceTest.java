package io.harness.ngpipeline.pipeline.resources;

import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.SANYASI_NAIDU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.common.RestQueryFilterParser;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineResponseDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineSummaryResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Files;
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
import org.springframework.data.mongodb.core.query.Criteria;

public class NGPipelineResourceTest extends CategoryTest {
  @Mock NGPipelineService ngPipelineService;
  @Mock RestQueryFilterParser restQueryFilterParser;
  NGPipelineResource ngPipelineResource;

  NGPipelineResponseDTO ngPipelineResponseDTO;
  NGPipelineSummaryResponseDTO ngPipelineSummaryResponseDTO;

  NgPipelineEntity ngPipelineEntity;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "k8s";
  private final String PIPELINE_NAME = "Manager Service Deployment";
  private List<NGTag> tags;

  @Before
  public void setUp() throws IOException {
    tags = Collections.singletonList(NGTag.builder().key("company").value("harness").build());
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = this.getClass().getClassLoader();
    File file = new File(classLoader.getResource("k8sPipeline.yaml").getFile());
    NgPipeline ngPipeline = YamlPipelineUtils.read(file.toURI().toURL(), NgPipeline.class);
    ngPipelineResource = new NGPipelineResource(ngPipelineService, restQueryFilterParser);
    ngPipelineResponseDTO = NGPipelineResponseDTO.builder()
                                .ngPipeline(ngPipeline)
                                .executionsPlaceHolder(new ArrayList<>())
                                .version(0L)
                                .build();
    ngPipelineEntity = NgPipelineEntity.builder()
                           .accountId(ACCOUNT_ID)
                           .projectIdentifier(PROJ_IDENTIFIER)
                           .orgIdentifier(ORG_IDENTIFIER)
                           .identifier(PIPELINE_IDENTIFIER)
                           .ngPipeline(ngPipeline)
                           .tags(tags)
                           .version(0L)
                           .build();
    ngPipelineSummaryResponseDTO = NGPipelineSummaryResponseDTO.builder()
                                       .identifier(PIPELINE_IDENTIFIER)
                                       .name(PIPELINE_NAME)
                                       .numOfStages(1)
                                       .tags(TagMapper.convertToMap(tags))
                                       .version(0L)
                                       .build();
  }

  @Test
  @Owner(developers = SANYASI_NAIDU)
  @Category(UnitTests.class)
  public void testGetPipeline() {
    doReturn(Optional.of(ngPipelineEntity))
        .when(ngPipelineService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
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
    doReturn(ngPipelineEntity).when(ngPipelineService).create(any());
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
    Criteria criteria = new Criteria();
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, PipelineNGKeys.createdAt));
    Page<NgPipelineEntity> pipelineList = new PageImpl<>(Collections.singletonList(ngPipelineEntity), pageable, 1);
    doReturn(criteria).when(restQueryFilterParser).getCriteriaFromFilterQuery(any(), any());
    doReturn(pipelineList).when(ngPipelineService).list(any(), any());

    List<NGPipelineSummaryResponseDTO> content =
        ngPipelineResource.getListOfPipelines("", "", "", "", 0, 10, null, "").getData().getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);

    NGPipelineSummaryResponseDTO responseDTO = content.get(0);
    assertThat(responseDTO.getIdentifier()).isEqualTo(ngPipelineSummaryResponseDTO.getIdentifier());
    assertThat(responseDTO.getName()).isEqualTo(ngPipelineSummaryResponseDTO.getName());
    assertThat(responseDTO.getDescription()).isEqualTo(ngPipelineSummaryResponseDTO.getDescription());
    assertThat(responseDTO.getNumOfStages()).isEqualTo(ngPipelineSummaryResponseDTO.getNumOfStages());
    assertThat(responseDTO.getTags()).isEqualTo(ngPipelineSummaryResponseDTO.getTags());
  }
}
