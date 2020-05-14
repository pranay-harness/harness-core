package io.harness.registries.resolver;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.resolvers.Resolver;
import io.harness.rule.Owner;
import io.harness.utils.DummyOutcome;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResolverRegistryTest extends OrchestrationBeansTest {
  @Inject private ResolverRegistry resolverRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    RefType refType = RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
    resolverRegistry.register(refType, SweepingOutputResolver.class);
    Resolver resolver = resolverRegistry.obtain(refType);
    assertThat(resolver).isNotNull();

    assertThatThrownBy(() -> resolverRegistry.register(refType, SweepingOutputResolver.class))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> resolverRegistry.obtain(RefType.builder().type("RANDOM").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(resolverRegistry.getType()).isEqualTo(RegistryType.RESOLVER);
  }

  @Value
  @Builder
  private static class SweepingOutputResolver implements Resolver<DummyOutcome> {
    @SuppressWarnings("unchecked")
    @Override
    public DummyOutcome resolve(Ambiance ambiance, RefObject refObject) {
      return null;
    }

    @Override
    public DummyOutcome consume(Ambiance ambiance, String name, DummyOutcome value) {
      return null;
    }

    @Override
    public RefType getType() {
      return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
    }
  }
}