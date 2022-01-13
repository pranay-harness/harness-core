/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.sm.states.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.BOGDAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class S3UriParserTest {
  public static final String BUCKET_NAME = "bucket-name";
  private S3UriParser validator;

  @Before
  public void setUp() {
    validator = new S3UriParser();
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParseUsEast1ShortFormPathStyle() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://s3.amazonaws.com/bucket-name/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParseUsEast1ShortFormVirtualHostedStyle() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://bucket-name.s3.amazonaws.com/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParseVirtualHostedDotRegionStyleUrls() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://bucket-name.s3.region-name.amazonaws.com/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParsePathStyleDotRegionUrls() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://s3.region-name.amazonaws.com/bucket-name/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParseVirtualHostedDashRegionUrls() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://bucket-name.s3-region-name.amazonaws.com/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldParsePathStyleDashRegionUrls() {
    // when
    Map<String, List<String>> bucketsFilesMap =
        validator.getBucketsFilesMap(Lists.newArrayList("https://s3-region-name.amazonaws.com/bucket-name/key"));

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldCombineTwoKeysInTheSameBucket() {
    // given
    ArrayList<String> paths = Lists.newArrayList("https://s3-region-name.amazonaws.com/bucket-name/key",
        "https://s3-region-name.amazonaws.com/bucket-name/another-key");

    // when
    Map<String, List<String>> bucketsFilesMap = validator.getBucketsFilesMap(paths);

    // then
    assertThat(bucketsFilesMap).hasFieldOrPropertyWithValue(BUCKET_NAME, Lists.newArrayList("key", "another-key"));
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionWhenUrlInvalid() {
    String invalidUrl = "not a valid url";
    assertThatThrownBy(() -> validator.getBucketsFilesMap(Lists.newArrayList(invalidUrl)))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("The " + invalidUrl + " format is not valid");
  }
}
