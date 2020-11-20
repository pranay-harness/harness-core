package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SplunkDataCollectionInfoTest extends CategoryTest {
  @Before
  public void setup() throws IOException {}

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDslEnvVariables() {
    SplunkDataCollectionInfo splunkDataCollectionInfo =
        SplunkDataCollectionInfo.builder().query("exception").serviceInstanceIdentifier("host").build();
    Map<String, Object> expected = new HashMap<>();
    expected.put("query", "exception");
    expected.put("serviceInstanceIdentifier", "$.host");
    expected.put("maxCount", 10000);
    expected.put("hostCollectionQuery", "host=*|stats count by host");
    assertThat(splunkDataCollectionInfo.getDslEnvVariables()).isEqualTo(expected);
  }
}
