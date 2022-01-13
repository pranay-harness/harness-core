/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PMSPipelineSummaryResponseDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.rule.Owner;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PMSPipelineDtoMapperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToEntity() {
    String acc = "acc";
    String org = "org1";
    String proj = "proj1";
    String yaml = "pipeline:\n"
        + "  identifier: p1\n"
        + "  name: p1\n"
        + "  description: desc\n"
        + "  orgIdentifier: org1\n"
        + "  projectIdentifier: proj1\n";
    PipelineEntity pipelineEntity = PMSPipelineDtoMapper.toPipelineEntity(acc, org, proj, yaml);
    assertThat(pipelineEntity.getIdentifier()).isEqualTo("p1");
    assertThat(pipelineEntity.getName()).isEqualTo("p1");
    assertThat(pipelineEntity.getAccountId()).isEqualTo("acc");
    assertThat(pipelineEntity.getOrgIdentifier()).isEqualTo("org1");
    assertThat(pipelineEntity.getProjectIdentifier()).isEqualTo("proj1");
    assertThat(pipelineEntity.getAllowStageExecutions()).isFalse();
    String yamlWithAllowExecutions = yaml + "  allowStageExecutions: true\n";
    assertThat(PMSPipelineDtoMapper.toPipelineEntity(acc, org, proj, yamlWithAllowExecutions).getAllowStageExecutions())
        .isTrue();
    String yamlWithDisallowExecutions = yaml + "  allowStageExecutions: false\n";
    assertThat(
        PMSPipelineDtoMapper.toPipelineEntity(acc, org, proj, yamlWithDisallowExecutions).getAllowStageExecutions())
        .isFalse();

    PipelineEntity pipelineEntity1 = PMSPipelineDtoMapper.toPipelineEntity(acc, yaml);
    assertThat(pipelineEntity1.getIdentifier()).isEqualTo("p1");
    assertThat(pipelineEntity1.getName()).isEqualTo("p1");
    assertThat(pipelineEntity1.getAccountId()).isEqualTo("acc");
    assertThat(pipelineEntity1.getOrgIdentifier()).isEqualTo("org1");
    assertThat(pipelineEntity1.getProjectIdentifier()).isEqualTo("proj1");
    assertThat(pipelineEntity1.getAllowStageExecutions()).isFalse();

    assertThat(PMSPipelineDtoMapper.toPipelineEntity(acc, yamlWithAllowExecutions).getAllowStageExecutions()).isTrue();
    assertThat(PMSPipelineDtoMapper.toPipelineEntity(acc, yamlWithDisallowExecutions).getAllowStageExecutions())
        .isFalse();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDeploymentsAndErrors() {
    Map<String, Integer> deploymentMap = new HashMap<>();
    Map<String, Integer> numberOfErrorMap = new HashMap<>();
    LocalDate todayDate = now();
    DateTimeFormatter formatters = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    for (int i = 0; i < 10; i++) {
      LocalDate variableDate = todayDate.minusDays(i);
      deploymentMap.put(variableDate.format(formatters), i + 10);
      numberOfErrorMap.put(variableDate.format(formatters), i);
    }
    List<Integer> deploymentList = new ArrayList<>();
    List<Integer> numberOfErrorsList = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate variableDate = todayDate.minusDays(i);
      deploymentList.add(deploymentMap.get(variableDate.format(formatters)));
      numberOfErrorsList.add(numberOfErrorMap.get(variableDate.format(formatters)));
    }
    PipelineEntity pipelineEntity =
        PipelineEntity.builder()
            .accountId("acc")
            .orgIdentifier("org")
            .projectIdentifier("pro")
            .executionSummaryInfo(
                ExecutionSummaryInfo.builder().deployments(deploymentMap).numOfErrors(numberOfErrorMap).build())
            .build();

    PMSPipelineSummaryResponseDTO pmsPipelineSummaryResponseDTO =
        PMSPipelineDtoMapper.preparePipelineSummary(pipelineEntity);

    assertThat(deploymentList).isEqualTo(pmsPipelineSummaryResponseDTO.getExecutionSummaryInfo().getDeployments());
    assertThat(numberOfErrorsList).isEqualTo(pmsPipelineSummaryResponseDTO.getExecutionSummaryInfo().getNumOfErrors());
  }
}
