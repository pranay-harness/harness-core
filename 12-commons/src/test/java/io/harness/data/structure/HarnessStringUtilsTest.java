package io.harness.data.structure;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class HarnessStringUtilsTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void shouldJoinIfIndividualElements() {
    String joinedString = HarnessStringUtils.join("/", "foo", "bar", "hello-world");
    Assertions.assertThat(joinedString).isEqualTo("foo/bar/hello-world");

    joinedString = HarnessStringUtils.join(StringUtils.EMPTY, "foo", "bar", "hello-world");
    Assertions.assertThat(joinedString).isEqualTo("foobarhello-world");
  }
  @Test
  @Category(UnitTests.class)
  public void shouldJoinIfIterableElements() {
    String joinedString = HarnessStringUtils.join("/", Arrays.asList("foo", "bar", "hello-world"));
    Assertions.assertThat(joinedString).isEqualTo("foo/bar/hello-world");

    joinedString = HarnessStringUtils.join(StringUtils.EMPTY,
        Arrays.asList("foo", "bar",
            "hello"
                + "-world"));
    Assertions.assertThat(joinedString).isEqualTo("foobarhello-world");
  }
}
