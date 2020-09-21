package io.harness.cdng.inputset.mappers;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class CDInputSetElementMapperTest extends CategoryTest {
  InputSetResponseDTO inputSetResponseDTO;
  InputSetSummaryResponseDTO inputSetSummaryResponseDTO;
  CDInputSetEntity requestCdInputSetEntity;
  CDInputSetEntity responseCdInputSetEntity;

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

    String inputSetFileName = "input-set-test-file.yaml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);

    CDInputSet inputSetObject = YamlPipelineUtils.read(inputSetYaml, CDInputSet.class);

    inputSetResponseDTO = InputSetResponseDTO.builder()
                              .accountId(ACCOUNT_ID)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJ_IDENTIFIER)
                              .identifier(IDENTIFIER)
                              .name(NAME)
                              .description(DESCRIPTION)
                              .pipelineIdentifier(PIPELINE_IDENTIFIER)
                              .inputSetYaml(inputSetYaml)
                              .build();

    inputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                     .identifier(IDENTIFIER)
                                     .name(NAME)
                                     .description(DESCRIPTION)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .isOverlaySet(false)
                                     .build();

    requestCdInputSetEntity = CDInputSetEntity.builder()
                                  .accountId(ACCOUNT_ID)
                                  .orgIdentifier(ORG_IDENTIFIER)
                                  .projectIdentifier(PROJ_IDENTIFIER)
                                  .identifier(IDENTIFIER)
                                  .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                  .inputSetYaml(inputSetYaml)
                                  .cdInputSet(inputSetObject)
                                  .name(NAME)
                                  .description(DESCRIPTION)
                                  .build();

    responseCdInputSetEntity = CDInputSetEntity.builder()
                                   .accountId(ACCOUNT_ID)
                                   .orgIdentifier(ORG_IDENTIFIER)
                                   .projectIdentifier(PROJ_IDENTIFIER)
                                   .identifier(IDENTIFIER)
                                   .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                   .inputSetYaml(inputSetYaml)
                                   .cdInputSet(inputSetObject)
                                   .name(NAME)
                                   .description(DESCRIPTION)
                                   .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntity() {
    CDInputSetEntity mappedInputSet = CDInputSetElementMapper.toCDInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestCdInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestCdInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestCdInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestCdInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestCdInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestCdInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestCdInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestCdInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntityWithIdentifier() {
    CDInputSetEntity mappedInputSet = CDInputSetElementMapper.toCDInputSetEntityWithIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, inputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestCdInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestCdInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestCdInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestCdInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestCdInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestCdInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestCdInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestCdInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteResponseDTO() {
    InputSetResponseDTO response = CDInputSetElementMapper.writeResponseDTO(responseCdInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteSummaryResponseDTO() {
    InputSetSummaryResponseDTO response = CDInputSetElementMapper.writeSummaryResponseDTO(responseCdInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(inputSetSummaryResponseDTO);
  }
}