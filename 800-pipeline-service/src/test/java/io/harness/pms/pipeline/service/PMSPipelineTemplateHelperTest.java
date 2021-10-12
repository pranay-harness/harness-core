package io.harness.pms.pipeline.service;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.template.TemplateApplyRequestDTO;
import io.harness.ng.core.template.TemplateInputsErrorResponseDTO;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.exception.NGTemplateResolveException;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.rule.Owner;
import io.harness.template.remote.TemplateResourceClient;

import java.io.IOException;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(HarnessTeam.PIPELINE)
public class PMSPipelineTemplateHelperTest extends CategoryTest {
  @Mock private PmsFeatureFlagHelper pmsFeatureFlagHelper;
  @Mock private TemplateResourceClient templateResourceClient;
  @InjectMocks private PMSPipelineTemplateHelper pipelineTemplateHelper;

  private static final String ACCOUNT_ID = "accountId";
  private static final String PROJECT_ID = "projectId";
  private static final String ORG_ID = "orgId";
  private static final String GIVEN_YAML = "yaml";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pipelineTemplateHelper = new PMSPipelineTemplateHelper(pmsFeatureFlagHelper, templateResourceClient);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testResolveTemplateRefsInPipelineWhenFFIsOff() {
    doReturn(false).when(pmsFeatureFlagHelper).isEnabled(ACCOUNT_ID, FeatureName.NG_TEMPLATES);
    String resolveTemplateRefsInPipeline =
        pipelineTemplateHelper.resolveTemplateRefsInPipeline(ACCOUNT_ID, ORG_ID, PROJECT_ID, GIVEN_YAML);
    assertThat(resolveTemplateRefsInPipeline).isEqualTo(GIVEN_YAML);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidTemplateInPipelineWhenFFIsOn() throws IOException {
    String mergedYaml = "MERGED_YAML";
    doReturn(true).when(pmsFeatureFlagHelper).isEnabled(ACCOUNT_ID, FeatureName.NG_TEMPLATES);
    Call<ResponseDTO<TemplateMergeResponseDTO>> callRequest = mock(Call.class);
    doReturn(callRequest)
        .when(templateResourceClient)
        .applyTemplatesOnGivenYaml(
            ACCOUNT_ID, ORG_ID, PROJECT_ID, TemplateApplyRequestDTO.builder().originalEntityYaml(GIVEN_YAML).build());
    when(callRequest.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            TemplateMergeResponseDTO.builder().mergedPipelineYaml(mergedYaml).valid(true).build())));
    String resolveTemplateRefsInPipeline =
        pipelineTemplateHelper.resolveTemplateRefsInPipeline(ACCOUNT_ID, ORG_ID, PROJECT_ID, GIVEN_YAML);
    assertThat(resolveTemplateRefsInPipeline).isEqualTo(mergedYaml);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testInValidTemplateInPipelineWhenFFIsOn() throws IOException {
    String invalidMergedYaml = "INVALID_MERGED_YAML";
    String errorYaml = "ERROR_YAML";
    doReturn(true).when(pmsFeatureFlagHelper).isEnabled(ACCOUNT_ID, FeatureName.NG_TEMPLATES);
    Call<ResponseDTO<TemplateMergeResponseDTO>> callRequest = mock(Call.class);
    doReturn(callRequest)
        .when(templateResourceClient)
        .applyTemplatesOnGivenYaml(
            ACCOUNT_ID, ORG_ID, PROJECT_ID, TemplateApplyRequestDTO.builder().originalEntityYaml(GIVEN_YAML).build());
    TemplateInputsErrorResponseDTO templateInputsErrorResponseDTO = new TemplateInputsErrorResponseDTO(
        Status.ERROR, ErrorCode.TEMPLATE_EXCEPTION, "Template Ref resolved failed.", "", errorYaml, new HashMap<>());
    when(callRequest.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(TemplateMergeResponseDTO.builder()
                                                                 .mergedPipelineYaml(invalidMergedYaml)
                                                                 .valid(false)
                                                                 .errorResponse(templateInputsErrorResponseDTO)
                                                                 .build())));
    assertThatThrownBy(
        () -> pipelineTemplateHelper.resolveTemplateRefsInPipeline(ACCOUNT_ID, ORG_ID, PROJECT_ID, GIVEN_YAML))
        .isInstanceOf(NGTemplateResolveException.class)
        .hasMessage("Exception in resolving template refs in given pipeline yaml.");
  }
}