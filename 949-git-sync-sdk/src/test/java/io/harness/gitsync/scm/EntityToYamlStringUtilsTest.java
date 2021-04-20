package io.harness.gitsync.scm;

import static io.harness.rule.OwnerRule.ABHINAV;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.utils.NGYamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
public class EntityToYamlStringUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlString() throws IOException {
    final SampleBean sampleBean = SampleBean.builder()
                                      .accountIdentifier("accountid")
                                      .test1("test1")
                                      .orgIdentifier("orgid")
                                      .projectIdentifier("projid")
                                      .build();
    final String yamlString = NGYamlUtils.getYamlString(sampleBean);
    String yaml = IOUtils.resourceToString("testYaml.yaml", UTF_8, this.getClass().getClassLoader());
    assertThat(yaml).isEqualTo(yamlString);
  }
}