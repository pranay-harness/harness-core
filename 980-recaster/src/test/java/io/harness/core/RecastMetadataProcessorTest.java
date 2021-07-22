package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.RecasterTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.metadata.RecastMetadataProcessor;
import io.harness.rule.Owner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class RecastMetadataProcessorTest extends RecasterTestBase {
  Recaster recaster = new Recaster();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void test() throws NoSuchFieldException {
    TestAnnotationClass testAnnotationClass = new TestAnnotationClass();
    Map<String, Object> map = recaster.toMap(testAnnotationClass);

    assertThat(map).isNotNull();
    asserttAnnotationSerialization(map);

    TestAnnotationClass recasted = recaster.fromMap(map, TestAnnotationClass.class);
    assertThat(recasted).isEqualTo(testAnnotationClass);
  }

  private void asserttAnnotationSerialization(Map<String, Object> map) throws NoSuchFieldException {
    Map<String, Object> metadata = (Map<String, Object>) map.get(RecastMetadataProcessor.METADATA);
    assertThat(metadata).isNotEmpty();

    Map<String, Object> annotations = (Map<String, Object>) metadata.get(RecastMetadataProcessor.ANNOTATIONS);
    assertThat(annotations).isNotEmpty();

    List<Annotation> nameAnnotations = (List<Annotation>) annotations.get("name");
    assertThat(nameAnnotations).isNotEmpty();
    RecastTest actualAnnotation = (RecastTest) nameAnnotations.get(0);

    RecastTest expectedAnnotation = TestAnnotationClass.class.getDeclaredField("name").getAnnotation(RecastTest.class);
    assertThat(actualAnnotation).isEqualTo(expectedAnnotation);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class TestAnnotationClass {
    @RecastTest(true) String name;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  private @interface RecastTest {
    boolean value();
  }
}
