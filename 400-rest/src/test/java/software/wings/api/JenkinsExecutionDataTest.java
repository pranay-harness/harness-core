/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JenkinsExecutionDataTest extends CategoryTest {
  private JenkinsExecutionData jenkinsExecutionData =
      JenkinsExecutionData.builder().jobName("testjob").buildUrl("http://jenkins/testjob/11").build();

  @Before
  public void setup() {
    jenkinsExecutionData.setErrorMsg("Err");
    jenkinsExecutionData.setJobStatus("ERROR");
    jenkinsExecutionData.setStatus(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    assertThat(jenkinsExecutionData.getExecutionSummary())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            ExecutionDataValue.builder().displayName("Job Name").value("testjob").build(), "build",
            ExecutionDataValue.builder().displayName("Build Url").value("http://jenkins/testjob/11").build(),
            "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value("ERROR").build()));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetExecutionDetails() {
    assertThat(jenkinsExecutionData.getExecutionDetails())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            ExecutionDataValue.builder().displayName("Job Name").value("testjob").build(), "build",
            ExecutionDataValue.builder().displayName("Build Url").value("http://jenkins/testjob/11").build(),
            "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value("ERROR").build()));
  }
}
