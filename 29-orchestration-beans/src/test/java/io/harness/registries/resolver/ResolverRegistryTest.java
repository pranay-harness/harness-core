package io.harness.registries.resolver;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.pms.refobjects.RefType;
import io.harness.references.OrchestrationRefType;
import io.harness.pms.refobjects.RefObject;
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

public class ResolverRegistryTest extends OrchestrationBeansTestBase {
  @Inject private ResolverRegistry resolverRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    RefType refType = RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build();
    resolverRegistry.register(refType, new SweepingOutputResolver());
    Resolver resolver = resolverRegistry.obtain(refType);
    assertThat(resolver).isNotNull();

    assertThatThrownBy(() -> resolverRegistry.register(refType, new SweepingOutputResolver()))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> resolverRegistry.obtain(RefType.newBuilder().setType("RANDOM").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(resolverRegistry.getType()).isEqualTo(RegistryType.RESOLVER.name());
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
    public String consumeInternal(Ambiance ambiance, String name, DummyOutcome value, int levelsToKeep) {
      return "id";
    }
  }
}
