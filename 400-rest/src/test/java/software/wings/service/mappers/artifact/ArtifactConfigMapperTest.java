/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.mappers.artifact;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactConfigMapperTest extends CategoryTest {
  BuildDetailsInternal buildDetailsInternal;
  @Before
  public void setUp() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("KEY", "VALUE");
    buildDetailsInternal = BuildDetailsInternal.builder()
                               .number("Tag1")
                               .uiDisplayName("TAG# Tag1")
                               .metadata(map)
                               .buildUrl("TAG_URL")
                               .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToBuildDetails() {
    BuildDetails buildDetails = ArtifactConfigMapper.toBuildDetails(buildDetailsInternal);
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("Tag1");
    assertThat(buildDetails.getUiDisplayName()).isEqualTo("TAG# Tag1");
    assertThat(buildDetails.getBuildUrl()).isEqualTo("TAG_URL");
    assertThat(buildDetails.getMetadata()).isEqualTo(buildDetailsInternal.getMetadata());
  }
}
