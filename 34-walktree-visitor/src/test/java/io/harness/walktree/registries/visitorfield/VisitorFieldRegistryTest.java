package io.harness.walktree.registries.visitorfield;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.WalkTreeBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VisitorFieldRegistryTest extends WalkTreeBaseTest {
  @Inject VisitorFieldRegistry visitorFieldRegistry;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    DummyVisitorField visitorField = new DummyVisitorField();
    visitorFieldRegistry.register(visitorField.getVisitorFieldType(), DummyVisitorFieldProcessor.class);
    VisitableFieldProcessor<?> processor = visitorFieldRegistry.obtain(visitorField.getVisitorFieldType());
    assertThat(processor).isNotNull();

    assertThatThrownBy(
        () -> visitorFieldRegistry.register(visitorField.getVisitorFieldType(), DummyVisitorFieldProcessor.class))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> visitorFieldRegistry.obtain(VisitorFieldType.builder().type("IGNORE").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(visitorFieldRegistry.getType()).isEqualTo("VISITOR_FIELD");
  }

  private static class DummyVisitorField implements VisitorFieldWrapper {
    @Override
    public VisitorFieldType getVisitorFieldType() {
      return VisitorFieldType.builder().type("DUMMY").build();
    }
  }

  private static class DummyVisitorFieldProcessor implements VisitableFieldProcessor<DummyVisitorField> {
    @Override
    public String getExpressionFieldValue(DummyVisitorField actualField) {
      return null;
    }

    @Override
    public DummyVisitorField updateCurrentField(DummyVisitorField actualField, DummyVisitorField overrideField) {
      return null;
    }

    @Override
    public DummyVisitorField createNewField(DummyVisitorField actualField) {
      return null;
    }

    @Override
    public String getFieldWithStringValue(DummyVisitorField actualField) {
      return null;
    }

    @Override
    public DummyVisitorField createNewFieldWithStringValue(String stringValue) {
      return null;
    }
  }
}