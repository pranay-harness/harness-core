package io.harness.ngpipeline.overlayinputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class OverlayInputSetElementMapperTest extends CategoryTest {
  OverlayInputSetResponseDTO overlayInputSetResponseDTO;
  OverlayInputSetEntity requestOverlayInputSetEntity;
  OverlayInputSetEntity responseOverlayInputSetEntity;

  private final String IDENTIFIER = "identifier";
  private final String PIPELINE_IDENTIFIER = "pipeline_identifier";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String NAME = "input set name";
  private final String DESCRIPTION = "input set description";
  private String inputSetYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String inputSetFileName = "overlay-input-set-test-file.yaml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);

    overlayInputSetResponseDTO = OverlayInputSetResponseDTO.builder()
                                     .accountId(ACCOUNT_ID)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .projectIdentifier(PROJ_IDENTIFIER)
                                     .identifier(IDENTIFIER)
                                     .name(NAME)
                                     .description(DESCRIPTION)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .overlayInputSetYaml(inputSetYaml)
                                     .build();

    requestOverlayInputSetEntity = OverlayInputSetEntity.builder()
                                       .accountId(ACCOUNT_ID)
                                       .orgIdentifier(ORG_IDENTIFIER)
                                       .projectIdentifier(PROJ_IDENTIFIER)
                                       .identifier(IDENTIFIER)
                                       .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                       .overlayInputSetYaml(inputSetYaml)
                                       .name(NAME)
                                       .description(DESCRIPTION)
                                       .build();

    responseOverlayInputSetEntity = OverlayInputSetEntity.builder()
                                        .accountId(ACCOUNT_ID)
                                        .orgIdentifier(ORG_IDENTIFIER)
                                        .projectIdentifier(PROJ_IDENTIFIER)
                                        .identifier(IDENTIFIER)
                                        .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                        .overlayInputSetYaml(inputSetYaml)
                                        .name(NAME)
                                        .description(DESCRIPTION)
                                        .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntity() {
    OverlayInputSetEntity mappedInputSet = OverlayInputSetElementMapper.toOverlayInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestOverlayInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestOverlayInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestOverlayInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestOverlayInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestOverlayInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestOverlayInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestOverlayInputSetEntity.getDescription());
    assertThat(mappedInputSet.getOverlayInputSetYaml())
        .isEqualTo(requestOverlayInputSetEntity.getOverlayInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntityWithIdentifier() {
    OverlayInputSetEntity mappedInputSet = OverlayInputSetElementMapper.toOverlayInputSetEntityWithIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, inputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestOverlayInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestOverlayInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestOverlayInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestOverlayInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestOverlayInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestOverlayInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestOverlayInputSetEntity.getDescription());
    assertThat(mappedInputSet.getOverlayInputSetYaml())
        .isEqualTo(requestOverlayInputSetEntity.getOverlayInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteResponseDTO() {
    OverlayInputSetResponseDTO response = OverlayInputSetElementMapper.writeResponseDTO(responseOverlayInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(overlayInputSetResponseDTO);
  }
}