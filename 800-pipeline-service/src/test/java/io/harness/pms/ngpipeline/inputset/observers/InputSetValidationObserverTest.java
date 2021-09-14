/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.ngpipeline.inputset.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.repositories.inputset.PMSInputSetRepository;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class InputSetValidationObserverTest extends PipelineServiceTestBase {
  @Mock PMSInputSetRepository inputSetRepository;
  @Mock PMSInputSetService inputSetService;
  @Mock ValidateAndMergeHelper validateAndMergeHelper;
  @InjectMocks InputSetValidationObserver inputSetValidationObserver;

  private static final String ACCOUNT_ID = "accountId";
  private static final String PROJECT_ID = "projectId";
  private static final String ORG_ID = "orgId";
  private static final String PIPELINE_ID = "pipelineId";
  private static String pipelineYaml = "pipeline:\n"
      + "    identifier: identifier\n"
      + "    name: pipeline1\n"
      + "    accountId: accountId\n"
      + "    orgIdentifier: orgId\n"
      + "    projectIdentifier: pipelineId\n"
      + "    tags: {}\n"
      + "    variables:\n"
      + "        - name: TITLE\n"
      + "          type: String\n"
      + "          value: <+input>";
  private static String inputSetYaml = "inputSet:\n"
      + "    name: input\n"
      + "    identifier: input\n"
      + "    orgIdentifier: orgId\n"
      + "    projectIdentifier: pipelineId\n"
      + "    pipeline:\n"
      + "        identifier: identifier\n"
      + "        variables:\n"
      + "        - name: TITLE\n"
      + "          type: String\n"
      + "          value: val";
  private static String inputSetYaml1 = "inputSet:\n"
      + "    name: input\n"
      + "    identifier: input\n"
      + "    orgIdentifier: orgId\n"
      + "    projectIdentifier: pipelineId\n"
      + "    pipeline:\n"
      + "        identifier: identifier\n"
      + "        abc: abc\n";

  PipelineEntity pipelineEntity;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pipelineEntity = PipelineEntity.builder()
                         .accountId(ACCOUNT_ID)
                         .orgIdentifier(ORG_ID)
                         .projectIdentifier(PROJECT_ID)
                         .identifier(PIPELINE_ID)
                         .yaml(pipelineYaml)
                         .build();
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testOnUpdate() {
    List<InputSetEntity> inputSetList = new ArrayList<>();
    inputSetList.add(InputSetEntity.builder().yaml(inputSetYaml).isInvalid(true).build());
    inputSetList.add(InputSetEntity.builder().yaml(inputSetYaml1).build());
    when(inputSetRepository.findAll(any())).thenReturn(inputSetList);
    assertThatCode(() -> inputSetValidationObserver.onUpdate(pipelineEntity)).doesNotThrowAnyException();
  }
}
