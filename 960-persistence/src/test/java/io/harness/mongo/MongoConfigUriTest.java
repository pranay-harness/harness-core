/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.mongo;

import static io.harness.rule.OwnerRule.FILIP;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.MongoConfig.HostAndPort;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MongoConfigUriTest extends CategoryTest {
  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWhenExists() {
    MongoConfig config = MongoConfig.builder().uri("whatever-we-pass").build();

    assertThat(config.getUri()).isEqualTo("whatever-we-pass");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnDefaultUriWhenNoneOfUriPartsIsSpecified() {
    MongoConfig config = MongoConfig.builder().build();

    assertThat(config.getUri()).isEqualTo(MongoConfig.DEFAULT_URI);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriFromPartsEvenWhenThereIsUriSpecified() {
    MongoConfig config =
        MongoConfig.builder().hosts(singletonList(HostAndPort.of("some-host", 123))).uri("this-is-ignored").build();

    assertThat(config.getUri()).isEqualTo("mongodb://some-host:123");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriSpecifiedByHostAndPort() {
    MongoConfig config = MongoConfig.builder().hosts(singletonList(HostAndPort.of("some-host", 123))).build();

    assertThat(config.getUri()).isEqualTo("mongodb://some-host:123");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriSpecifiedByHostAndDatabase() {
    MongoConfig config =
        MongoConfig.builder().hosts(singletonList(HostAndPort.of("some-host"))).database("example").build();

    assertThat(config.getUri()).isEqualTo("mongodb://some-host/example");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithUsernameFromUriParts() {
    MongoConfig config =
        MongoConfig.builder().hosts(singletonList(HostAndPort.of("host"))).username("user555").database("db").build();

    assertThat(config.getUri()).isEqualTo("mongodb://user555@host/db");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithUsernameAndPassowrdFromUriParts() {
    MongoConfig config = MongoConfig.builder()
                             .hosts(singletonList(HostAndPort.of("host", 432)))
                             .username("user123")
                             .password("strong-pass")
                             .build();

    assertThat(config.getUri()).isEqualTo("mongodb://user123:strong-pass@host:432");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithSingleQueryParam() {
    MongoConfig config = MongoConfig.builder()
                             .hosts(singletonList(HostAndPort.of("host", 9776)))
                             .params(Collections.singletonMap("param", "value"))
                             .build();

    assertThat(config.getUri()).isEqualTo("mongodb://host:9776/?param=value");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithMultipleQueryParam() {
    MongoConfig config = MongoConfig.builder()
                             .hosts(singletonList(HostAndPort.of("host", 9776)))
                             .params(ImmutableMap.of("key123", "val123", "param555", "value555"))
                             .build();

    assertThat(config.getUri())
        .startsWith("mongodb://host:9776/?")
        .containsOnlyOnce("key123=val123")
        .containsOnlyOnce("param555=value555");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriBuiltUsingAllUriParts() {
    MongoConfig config =
        MongoConfig.builder()
            .hosts(ImmutableList.of(HostAndPort.of("hostname1", 23456), HostAndPort.of("hostname2", 65432)))
            .password("super-pass")
            .username("dbuser")
            .database("test")
            .params(ImmutableMap.of("key123", "val123", "param555", "value555"))
            .build();

    assertThat(config.getUri())
        .startsWith("mongodb://dbuser:super-pass@hostname1:23456,hostname2:65432/test?")
        .containsOnlyOnce("key123=val123")
        .containsOnlyOnce("param555=value555");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriBuiltUsingUnusualCharacters() {
    MongoConfig config = MongoConfig.builder()
                             .hosts(singletonList(HostAndPort.of("host123456789", 23456)))
                             .password("super-pass!@#$%^&*()_+±")
                             .username("db/[]?@user")
                             .database("test")
                             .params(ImmutableMap.of("key123", "val123", "param555", "value555"))
                             .build();

    assertThat(config.getUri())
        .startsWith("mongodb://db%2F%5B%5D%3F%40user:super-pass!%40%23$%25%5E&*()_+%C2%B1@host123456789:23456/test?")
        .containsOnlyOnce("key123=val123")
        .containsOnlyOnce("param555=value555");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithMultipleHosts() {
    MongoConfig config = MongoConfig.builder()
                             .hosts(ImmutableList.of(HostAndPort.of("host_1", 111), HostAndPort.of("host_2"),
                                 HostAndPort.of("host_3", 333)))
                             .build();

    assertThat(config.getUri()).isEqualTo("mongodb://host_1:111,host_2,host_3:333");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithUserPassAndMultipleHosts() {
    MongoConfig config = MongoConfig.builder()
                             .username("user")
                             .password("pass")
                             .hosts(ImmutableList.of(HostAndPort.of("host_1", 111), HostAndPort.of("host_2", 222),
                                 HostAndPort.of("host_3")))
                             .database("db")
                             .build();

    assertThat(config.getUri()).isEqualTo("mongodb://user:pass@host_1:111,host_2:222,host_3/db");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReturnUriWithSpecifiedSchema() {
    MongoConfig config =
        MongoConfig.builder().schema("mongodb+srv").hosts(singletonList(HostAndPort.of("some-host", 123))).build();

    assertThat(config.getUri()).isEqualTo("mongodb+srv://some-host:123");
  }
}
