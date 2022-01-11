/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpConnectionExecutionCapabilityTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testFetchCapabilityBasis() {
    HttpConnectionExecutionCapability original = HttpConnectionExecutionCapability.builder().url("dummy").build();
    assertThat(original.fetchCapabilityBasis()).isEqualTo("dummy");

    HttpConnectionExecutionCapability noPortPath =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(-1).build();
    assertThat(noPortPath.fetchCapabilityBasis()).isEqualTo("http://domain");

    HttpConnectionExecutionCapability noPort =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(-1).path("path").build();
    assertThat(noPort.fetchCapabilityBasis()).isEqualTo("http://domain/path");

    HttpConnectionExecutionCapability noPath =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(80).build();
    assertThat(noPath.fetchCapabilityBasis()).isEqualTo("http://domain:80");

    HttpConnectionExecutionCapability all = HttpConnectionExecutionCapability.builder()
                                                .scheme("http")
                                                .host("domain")
                                                .port(80)
                                                .path("path")
                                                .query("query=queryString")
                                                .build();
    assertThat(all.fetchCapabilityBasis()).isEqualTo("http://domain:80/path?query=queryString");

    HttpConnectionExecutionCapability emptyQuery = HttpConnectionExecutionCapability.builder()
                                                       .scheme("http")
                                                       .host("domain")
                                                       .port(80)
                                                       .path("path")
                                                       .query("")
                                                       .build();
    assertThat(emptyQuery.fetchCapabilityBasis()).isEqualTo("http://domain:80/path");

    HttpConnectionExecutionCapability urlEncodedQuery = HttpConnectionExecutionCapability.builder()
                                                            .scheme("http")
                                                            .host("domain")
                                                            .port(80)
                                                            .path("path")
                                                            .query("query=query string require url encoding")
                                                            .build();
    assertThat(urlEncodedQuery.fetchCapabilityBasis())
        .isEqualTo("http://domain:80/path?query=query%20string%20require%20url%20encoding");

    assertThat(emptyQuery.fetchCapabilityBasis()).isEqualTo("http://domain:80/path");

    HttpConnectionExecutionCapability urlEncodedQueryWithoutPath = HttpConnectionExecutionCapability.builder()
                                                                       .scheme("http")
                                                                       .host("domain")
                                                                       .port(80)
                                                                       .query("query=query string require url encoding")
                                                                       .build();
    assertThat(urlEncodedQueryWithoutPath.fetchCapabilityBasis())
        .isEqualTo("http://domain:80?query=query%20string%20require%20url%20encoding");
  }
}
