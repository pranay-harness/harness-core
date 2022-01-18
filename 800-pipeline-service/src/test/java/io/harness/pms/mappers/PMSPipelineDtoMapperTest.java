/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
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
