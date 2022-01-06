/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Charsets;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class GitChangeSetMapperTest extends CategoryTest {
  @InjectMocks GitChangeSetMapper gitChangeSetMapper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_JsonNode() throws IOException {
    final String s = IOUtils.resourceToString("yaml/testyaml.yaml", Charsets.UTF_8, this.getClass().getClassLoader());
    final JsonNode jsonNode = gitChangeSetMapper.convertYamlToJsonNode(s);
    final String projectIdentifier = GitChangeSetMapper.getKeyInNode(jsonNode, "projectIdentifier");
    assertThat(projectIdentifier).isEqualTo("Harness Sample App");
  }
}
