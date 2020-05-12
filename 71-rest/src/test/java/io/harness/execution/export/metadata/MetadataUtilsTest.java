package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.export.metadata.MetadataTestHelper.SimpleVisitor;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MetadataUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testMap() {
    assertThat(MetadataUtils.map(null, Function.identity())).isNull();
    assertThat(MetadataUtils.map(Collections.emptyList(), Function.identity())).isNull();
    assertThat(MetadataUtils.map(Collections.singletonList("s"), MetadataUtilsTest::mappingFn)).isNull();

    List<String> output = MetadataUtils.map(asList("a", "s", "b"), MetadataUtilsTest::mappingFn);
    assertThat(output).isNotNull();
    assertThat(output.size()).isEqualTo(2);
    assertThat(output).containsExactly("a", "b");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testDedup() {
    assertThat(MetadataUtils.dedup(null, Function.identity())).isNull();
    assertThat(MetadataUtils.dedup(Collections.emptyList(), Function.identity())).isNull();
    assertThat(MetadataUtils.dedup(new ArrayList<>(Collections.singletonList("s")), MetadataUtilsTest::mappingFn))
        .isNull();

    List<String> output =
        MetadataUtils.dedup(new ArrayList<>(asList("a", "s", "b", "a", "a", "b", "c")), MetadataUtilsTest::mappingFn);
    assertThat(output).isNotNull();
    assertThat(output.size()).isEqualTo(3);
    assertThat(output).containsExactly("a", "b", "c");
  }

  private static String mappingFn(String str) {
    return str.equals("s") ? null : str;
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAcceptMultiple() {
    SimpleVisitor simpleVisitor = new SimpleVisitor();
    MetadataUtils.acceptMultiple(simpleVisitor, null);
    assertThat(simpleVisitor.getVisited().isEmpty()).isTrue();

    simpleVisitor = new SimpleVisitor();
    MetadataUtils.acceptMultiple(simpleVisitor,
        asList(GraphNodeMetadata.builder().id("id1").build(), null, GraphNodeMetadata.builder().id("id2").build()));
    assertThat(simpleVisitor.getVisited().size()).isEqualTo(2);
    assertThat(simpleVisitor.getVisited()).containsExactly("id1", "id2");
  }
}
