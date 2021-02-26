package io.harness.gitsync.common.helper;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class GitSyncUtilsTest extends CategoryTest {
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void getEntityTypeFromYaml() throws IOException {
    final String s =
        IOUtils.resourceToString("yaml/testyaml.yaml", StandardCharsets.UTF_8, getClass().getClassLoader());
    final EntityType entityTypeFromYaml = GitSyncUtils.getEntityTypeFromYaml(s);
    assertThat(entityTypeFromYaml).isEqualTo(EntityType.CONNECTORS);
  }
}