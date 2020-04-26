package io.harness.registries.resolver;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.resolvers.Resolver;
import io.harness.rule.Owner;
import io.harness.state.io.StateTransput;
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
    resolverRegistry.register(refType, new SweepingOutputResolverProducer());
    Resolver resolver = resolverRegistry.obtain(refType);
    assertThat(resolver).isNotNull();

    assertThatThrownBy(() -> resolverRegistry.register(refType, new SweepingOutputResolverProducer()))
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

  private static class SweepingOutputResolverProducer implements ResolverProducer {
    @Override
    public Resolver produceResolver() {
      return new SweepingOutputResolver();
    }

    @Override
    public RefType getType() {
      return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
    }
  }

  @Value
  @Builder
  private static class SweepingOutputResolver implements Resolver {
    @Override
    public StateTransput resolve(RefObject refObject) {
      return null;
    }
  }
}