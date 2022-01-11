/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.impl.webhookevent;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class GitPushEventExecutionServiceImplTest extends CategoryTest {
  @InjectMocks private GitPushEventExecutionServiceImpl gitPushEventExecutionService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_theRegexForBranchName() {
    String branchWithoutSlash = "refs/heads/test";
    final String branchName = gitPushEventExecutionService.getBranchName(
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().setRef(branchWithoutSlash).build()).build());
    assertThat(branchName).isEqualTo("test");
    String branchWithSlash = "refs/heads/harness/test";
    final String branchNameWithSlash = gitPushEventExecutionService.getBranchName(
        ParseWebhookResponse.newBuilder().setPush(PushHook.newBuilder().setRef(branchWithSlash).build()).build());
    assertThat(branchNameWithSlash).isEqualTo("harness/test");
  }
}
