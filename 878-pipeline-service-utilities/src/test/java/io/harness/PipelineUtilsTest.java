/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PipelineUtilsTest extends CategoryTest {
  private PipelineUtils pipelineUtils = new PipelineUtils();

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetBuildDetailsURL() {
    String accountId = "accountId";
    String orgId = "orgId";
    String projectId = "projectId";
    String pipelineId = "pipelineId";
    String executionId = "executionId";
    String baseUrl = "localhost";
    String url = baseUrl + "/account/" + accountId + "/ci/orgs/" + orgId + "/projects/" + projectId + "/pipelines/"
        + pipelineId + "/executions/" + executionId + "/pipeline";
    NGAccess ngAccess =
        BaseNGAccess.builder().accountIdentifier(accountId).orgIdentifier(orgId).projectIdentifier(projectId).build();
    String returnedUrl = pipelineUtils.getBuildDetailsUrl(ngAccess, pipelineId, executionId, baseUrl);
    assertEquals(url, returnedUrl);
  }
}
