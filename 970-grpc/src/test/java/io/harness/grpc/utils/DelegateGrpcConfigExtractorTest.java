/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateGrpcConfigExtractorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractTargetGivenUrlWithPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io/ccm";
    assertThat(DelegateGrpcConfigExtractor.extractTarget(managerUrl)).isEqualTo("pr.harness.io");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractTargetGivenUrlWithoutPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io";
    assertThat(DelegateGrpcConfigExtractor.extractTarget(managerUrl)).isEqualTo("pr.harness.io");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractAuthorityGivenUrlWithPrefix() throws Exception {
    // {prefix}-{svc}-{grpc}-{env}.harness.io
    String managerUrl = "https://pr.harness.io/ccm";
    assertThat(DelegateGrpcConfigExtractor.extractAuthority(managerUrl, "events"))
        .isEqualTo("ccm-events-grpc-pr.harness.io");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldExtractAuthorityGivenUrlWithoutPrefix() throws Exception {
    String managerUrl = "https://pr.harness.io";
    assertThat(DelegateGrpcConfigExtractor.extractAuthority(managerUrl, "events"))
        .isEqualTo("events-grpc-pr.harness.io");
  }
}
