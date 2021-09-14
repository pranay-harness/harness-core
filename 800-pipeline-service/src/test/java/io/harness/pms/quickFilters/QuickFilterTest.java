/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.quickFilters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.service.FilterService;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.helpers.TriggeredByHelper;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;
import io.harness.pms.plan.execution.service.PMSExecutionServiceImpl;
import io.harness.rule.Owner;

import java.util.Collections;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public class QuickFilterTest extends CategoryTest {
  @Mock private TriggeredByHelper triggeredByHelper;
  @Mock private FilterService filterService;

  @InjectMocks private PMSExecutionServiceImpl pmsExecutionServiceImpl;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    when(triggeredByHelper.getFromSecurityContext()).thenReturn(TriggeredBy.newBuilder().build());
  }
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFormCriteriaQuickFilters() {
    // testing pipelineIdentifier,status and myDeployments values
    Criteria form = pmsExecutionServiceImpl.formCriteria(
        "acc", "org", "pro", "pip", null, null, "mod", "sear", ExecutionStatus.FAILED, true, false, null);

    // status
    assertThat(form.getCriteriaObject().get("status").toString().contentEquals(ExecutionStatus.FAILED.name()))
        .isEqualTo(true);

    // myDeployments
    assertThat(form.getCriteriaObject().containsKey("executionTriggerInfo")).isEqualTo(true);

    // pipelineIdentifier
    assertThat(form.getCriteriaObject().get("pipelineIdentifier").toString().contentEquals("pip")).isEqualTo(true);

    // pipelineDeleted
    assertThat(form.getCriteriaObject().get("pipelineDeleted")).isNotEqualTo(true);

    // making myDeployments = false
    Criteria allDeploymentsForm = pmsExecutionServiceImpl.formCriteria(
        "acc", "org", "pro", "pip", null, null, "mod", "sear", ExecutionStatus.FAILED, false, false, null);
    // allDeployment -> myDeployments = false
    assertThat(allDeploymentsForm.getCriteriaObject().containsKey("executionTriggerInfo")).isEqualTo(false);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFormCriteriaFilterProperties() {
    // making a filterProperties object with a status value
    Criteria form = pmsExecutionServiceImpl.formCriteria(null, null, null, null, null,
        PipelineExecutionFilterPropertiesDTO.builder()
            .status(Collections.singletonList(ExecutionStatus.ABORTED))
            .build(),
        null, null, null, true, false, null);
    String documentString = "[Document{{status=Document{{$in=[ABORTED]}}}}, Document{{}}, Document{{}}, Document{{}}]";
    assertThat(form.getCriteriaObject().get("$and").toString()).isEqualTo(documentString);

    // filterProperties and filterIdentifier as not null
    assertThatThrownBy(()
                           -> pmsExecutionServiceImpl.formCriteria("acc", "org", "pro", "pip", "filterIdentifierDummy",
                               PipelineExecutionFilterPropertiesDTO.builder()
                                   .status(Collections.singletonList(ExecutionStatus.ABORTED))
                                   .build(),
                               "mod", "sear", null, true, false, null))
        .isInstanceOf(InvalidRequestException.class);

    // giving random name to filterIdentifier and filterProperties as null
    String randomFilterIdentifier = RandomStringUtils.randomAlphabetic(10);
    assertThatThrownBy(()
                           -> pmsExecutionServiceImpl.formCriteria("acc", "org", "pro", "pip", randomFilterIdentifier,
                               null, "mod", "sear", null, true, false, null))
        .isInstanceOf(InvalidRequestException.class);
  }
}
