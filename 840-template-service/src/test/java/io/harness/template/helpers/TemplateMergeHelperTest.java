/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.TemplateServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.TemplateReferenceSummary;
import io.harness.ng.core.template.exception.NGTemplateResolveException;
import io.harness.rule.Owner;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.services.NGTemplateService;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
public class TemplateMergeHelperTest extends TemplateServiceTestBase {
  @InjectMocks private TemplateMergeHelper templateMergeHelper;
  @Mock private NGTemplateService templateService;

  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_ID = "orgId";
  private static final String PROJECT_ID = "projectId";

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCreateTemplateInputsFromStepTemplateWithRuntimeInputs() {
    String filename = "template-step.yaml";
    String yaml = readFile(filename);
    String templateYaml = templateMergeHelper.createTemplateInputsFromTemplate(yaml);
    assertThat(templateYaml).isNotNull();

    String resFile = "template-step-templateInputs.yaml";
    String resTemplate = readFile(resFile);
    assertThat(templateYaml).isEqualTo(resTemplate);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testCreateTemplateInputsFromStepTemplateWithoutRuntimeInputs() {
    String filename = "step-template-without-runtime-inputs.yaml";
    String yaml = readFile(filename);
    String templateYaml = templateMergeHelper.createTemplateInputsFromTemplate(yaml);
    assertThat(templateYaml).isNullOrEmpty();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMergeTemplateSpecToPipelineYaml_StepTemplateAtDiffScope() {
    String filename = "template-step.yaml";
    String shellScriptTemplateStepYaml = readFile(filename);
    TemplateEntity templateEntity = TemplateEntity.builder()
                                        .accountId(ACCOUNT_ID)
                                        .orgIdentifier(ORG_ID)
                                        .projectIdentifier(PROJECT_ID)
                                        .yaml(shellScriptTemplateStepYaml)
                                        .deleted(false)
                                        .versionLabel("1")
                                        .build();

    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, null, "template1", "1", false))
        .thenReturn(Optional.of(templateEntity));
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template1", "", false))
        .thenReturn(Optional.of(templateEntity));

    String approvalTemplateStepYaml = readFile("approval-step-template.yaml");
    TemplateEntity approvalTemplateEntity = TemplateEntity.builder()
                                                .accountId(ACCOUNT_ID)
                                                .orgIdentifier(ORG_ID)
                                                .projectIdentifier(PROJECT_ID)
                                                .yaml(approvalTemplateStepYaml)
                                                .deleted(false)
                                                .build();
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, null, null, "template2", "1", false))
        .thenReturn(Optional.of(approvalTemplateEntity));

    String pipelineYamlFile = "pipeline-with-template-step-diff-scope.yaml";
    String pipelineYaml = readFile(pipelineYamlFile);
    TemplateMergeResponseDTO pipelineMergeResponse =
        templateMergeHelper.mergeTemplateSpecToPipelineYaml(ACCOUNT_ID, ORG_ID, PROJECT_ID, pipelineYaml);
    String finalPipelineYaml = pipelineMergeResponse.getMergedPipelineYaml();
    assertThat(finalPipelineYaml).isNotNull();
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).isNotNull();
    List<TemplateReferenceSummary> templateReferenceSummaryList = new ArrayList<>();
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template1")
                                         .versionLabel("1")
                                         .scope(Scope.ORG)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.shellScriptStep11")
                                         .build());
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template1")
                                         .versionLabel("1")
                                         .scope(Scope.PROJECT)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.shellScriptStep12")
                                         .build());
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template2")
                                         .versionLabel("1")
                                         .scope(Scope.ACCOUNT)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.approval")
                                         .build());
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).hasSize(3);
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).containsAll(templateReferenceSummaryList);

    String resFile = "pipeline-with-template-step-replaced.yaml";
    String resPipeline = readFile(resFile);
    assertThat(finalPipelineYaml).isEqualTo(resPipeline);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMergeTemplateSpecToPipelineYaml_StepTemplate() {
    String filename = "template-step.yaml";
    String shellScriptTemplateStepYaml = readFile(filename);
    TemplateEntity templateEntity = TemplateEntity.builder()
                                        .accountId(ACCOUNT_ID)
                                        .orgIdentifier(ORG_ID)
                                        .projectIdentifier(PROJECT_ID)
                                        .yaml(shellScriptTemplateStepYaml)
                                        .identifier("template1")
                                        .deleted(false)
                                        .versionLabel("1")
                                        .build();

    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template1", "1", false))
        .thenReturn(Optional.of(templateEntity));
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template1", "", false))
        .thenReturn(Optional.of(templateEntity));

    String approvalTemplateStepYaml = readFile("approval-step-template.yaml");
    TemplateEntity approvalTemplateEntity = TemplateEntity.builder()
                                                .accountId(ACCOUNT_ID)
                                                .orgIdentifier(ORG_ID)
                                                .projectIdentifier(PROJECT_ID)
                                                .yaml(approvalTemplateStepYaml)
                                                .identifier("template2")
                                                .versionLabel("1")
                                                .deleted(false)
                                                .build();
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template2", "1", false))
        .thenReturn(Optional.of(approvalTemplateEntity));

    String pipelineYamlFile = "pipeline-with-template-step.yaml";
    String pipelineYaml = readFile(pipelineYamlFile);
    TemplateMergeResponseDTO pipelineMergeResponse =
        templateMergeHelper.mergeTemplateSpecToPipelineYaml(ACCOUNT_ID, ORG_ID, PROJECT_ID, pipelineYaml);
    String finalPipelineYaml = pipelineMergeResponse.getMergedPipelineYaml();
    assertThat(finalPipelineYaml).isNotNull();
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).isNotNull();
    List<TemplateReferenceSummary> templateReferenceSummaryList = new ArrayList<>();
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template1")
                                         .versionLabel("1")
                                         .scope(Scope.PROJECT)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.shellScriptStep11")
                                         .build());
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template1")
                                         .versionLabel("1")
                                         .scope(Scope.PROJECT)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.shellScriptStep12")
                                         .build());
    templateReferenceSummaryList.add(TemplateReferenceSummary.builder()
                                         .templateIdentifier("template2")
                                         .versionLabel("1")
                                         .scope(Scope.PROJECT)
                                         .fqn("pipeline.stages.qaStage.spec.execution.steps.approval")
                                         .build());
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).hasSize(3);
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).containsAll(templateReferenceSummaryList);

    String resFile = "pipeline-with-template-step-replaced.yaml";
    String resPipeline = readFile(resFile);
    assertThat(finalPipelineYaml).isEqualTo(resPipeline);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMergeTemplateSpecToPipelineYaml_StageTemplate() {
    String stageTemplateFileName = "stage-template.yaml";
    String stageTemplateYaml = readFile(stageTemplateFileName);
    TemplateEntity stageTemplateEntity = TemplateEntity.builder()
                                             .accountId(ACCOUNT_ID)
                                             .orgIdentifier(ORG_ID)
                                             .projectIdentifier(PROJECT_ID)
                                             .yaml(stageTemplateYaml)
                                             .deleted(false)
                                             .build();

    String httpTemplateFileName = "http-step-template.yaml";
    String httpTemplateYaml = readFile(httpTemplateFileName);
    TemplateEntity httpTemplateEntity = TemplateEntity.builder()
                                            .accountId(ACCOUNT_ID)
                                            .orgIdentifier(ORG_ID)
                                            .projectIdentifier(PROJECT_ID)
                                            .yaml(httpTemplateYaml)
                                            .deleted(false)
                                            .build();

    String approvalTemplateFileName = "approval-step-template.yaml";
    String approvalTemplateYaml = readFile(approvalTemplateFileName);
    TemplateEntity approvalTemplateEntity = TemplateEntity.builder()
                                                .accountId(ACCOUNT_ID)
                                                .orgIdentifier(ORG_ID)
                                                .projectIdentifier(PROJECT_ID)
                                                .yaml(approvalTemplateYaml)
                                                .deleted(false)
                                                .build();

    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "stageTemplate", "1", false))
        .thenReturn(Optional.of(stageTemplateEntity));
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "httpTemplate", "1", false))
        .thenReturn(Optional.of(httpTemplateEntity));
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "approvalTemplate", "1", false))
        .thenReturn(Optional.of(approvalTemplateEntity));

    String pipelineYamlFile = "pipeline-with-stage-template.yaml";
    String pipelineYaml = readFile(pipelineYamlFile);
    TemplateMergeResponseDTO pipelineMergeResponse =
        templateMergeHelper.mergeTemplateSpecToPipelineYaml(ACCOUNT_ID, ORG_ID, PROJECT_ID, pipelineYaml);
    String finalPipelineYaml = pipelineMergeResponse.getMergedPipelineYaml();
    assertThat(finalPipelineYaml).isNotNull();
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries()).isNotNull().hasSize(1);
    assertThat(pipelineMergeResponse.getTemplateReferenceSummaries())
        .contains(TemplateReferenceSummary.builder()
                      .templateIdentifier("stageTemplate")
                      .versionLabel("1")
                      .scope(Scope.PROJECT)
                      .fqn("pipeline.stages.qaStage")
                      .build());

    String resFile = "pipeline-with-stage-template-replaced.yaml";
    String resPipeline = readFile(resFile);
    assertThat(finalPipelineYaml).isEqualTo(resPipeline);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMergeTemplateSpecToPipelineYaml_InvalidStepTemplate() {
    String filename = "template-step.yaml";
    String shellScriptTemplateStepYaml = readFile(filename);
    TemplateEntity templateEntity = TemplateEntity.builder()
                                        .accountId(ACCOUNT_ID)
                                        .orgIdentifier(ORG_ID)
                                        .projectIdentifier(PROJECT_ID)
                                        .yaml(shellScriptTemplateStepYaml)
                                        .deleted(false)
                                        .build();

    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template1", "1", false))
        .thenReturn(Optional.of(templateEntity));

    String approvalTemplateStepYaml = readFile("approval-step-template-without-runtime-inputs.yaml");
    TemplateEntity approvalTemplateEntity = TemplateEntity.builder()
                                                .accountId(ACCOUNT_ID)
                                                .orgIdentifier(ORG_ID)
                                                .projectIdentifier(PROJECT_ID)
                                                .yaml(approvalTemplateStepYaml)
                                                .deleted(false)
                                                .build();
    when(templateService.getOrThrowExceptionIfInvalid(ACCOUNT_ID, ORG_ID, PROJECT_ID, "template2", "2", false))
        .thenReturn(Optional.of(approvalTemplateEntity));

    String pipelineYamlFile = "pipeline-with-invalid-template-steps.yaml";
    String pipelineYaml = readFile(pipelineYamlFile);
    try {
      templateMergeHelper.mergeTemplateSpecToPipelineYaml(ACCOUNT_ID, ORG_ID, PROJECT_ID, pipelineYaml);
    } catch (NGTemplateResolveException ngTemplateResolveException) {
      assertThat(ngTemplateResolveException.getErrorResponseDTO()).isNotNull();
      assertThat(ngTemplateResolveException.getErrorResponseDTO().getErrorMap()).hasSize(3);
    }
  }
}
