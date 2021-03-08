package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.schema.beans.OneOfMapping;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JacksonClassHelperTest extends CategoryTest {
  JacksonClassHelper jacksonSubtypeHelper = new JacksonClassHelper();

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSubtypeMapping() {
    Map<String, SwaggerDefinitionsMetaInfo> stringModelSet = new HashMap<>();
    jacksonSubtypeHelper.getRequiredMappings(
        io.harness.yaml.TestClass.ClassWhichContainsInterface.class, stringModelSet);
    assertThat(stringModelSet).isNotEmpty();
    assertThat(stringModelSet.size()).isEqualTo(4);
    assertThat(stringModelSet.get("ClassWhichContainsInterface").getSubtypeClassMap().size()).isEqualTo(1);
    assertThat(stringModelSet.get("testName").getOneOfMappings().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetOneOfMappingsForClass() {
    final Set<OneOfMapping> oneOfMappingsForClass =
        jacksonSubtypeHelper.getOneOfMappingsForClass(io.harness.yaml.TestClass.ClassWithApiModelOverride.class);
    final OneOfMapping oneOfMapping_1 =
        OneOfMapping.builder().oneOfFieldNames(new HashSet<>(Arrays.asList("a", "b"))).nullable(false).build();
    final OneOfMapping oneOfMapping_2 =
        OneOfMapping.builder()
            .oneOfFieldNames(new HashSet<>(Arrays.asList("jsontypeinfo", "apimodelproperty")))
            .nullable(false)
            .build();
    assertThat(oneOfMappingsForClass).containsExactlyInAnyOrder(oneOfMapping_1, oneOfMapping_2);

    final Set<OneOfMapping> oneOfMappingsForClass_1 =
        jacksonSubtypeHelper.getOneOfMappingsForClass(TestClass.ClassWithoutApiModelOverride.class);
    final OneOfMapping oneOfMapping_1_1 =
        OneOfMapping.builder().oneOfFieldNames(new HashSet<>(Arrays.asList("x", "y"))).nullable(false).build();
    assertThat(oneOfMappingsForClass_1).containsExactlyInAnyOrder(oneOfMapping_1_1);
  }
}