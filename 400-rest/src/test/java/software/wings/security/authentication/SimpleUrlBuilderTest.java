/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SimpleUrlBuilderTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testBuildUrl() {
    String url = new SimpleUrlBuilder("http://google.com").addQueryParam("a", "1").addQueryParam("b", "2").build();

    assertThat(url).isEqualTo("http://google.com?a=1&b=2");
  }
}
