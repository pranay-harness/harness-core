package io.harness;

import io.harness.ambiance.AmbianceTest;
import io.harness.ambiance.AmbianceUtilsTest;
import io.harness.facilitator.modes.chain.child.ChildChainResponseTest;
import io.harness.plan.PlanTest;
import io.harness.plan.input.InputArgsTest;
import io.harness.plan.input.InputSetTest;
import io.harness.registries.adviser.AdviserRegistryTest;
import io.harness.registries.events.OrchestrationEventHandlerRegistryTest;
import io.harness.registries.facilitator.FacilitatorRegistryTest;
import io.harness.registries.resolver.ResolverRegistryTest;
import io.harness.registries.state.StepRegistryTest;
import io.harness.runners.GuiceSuiteRunner;
import io.harness.runners.ModuleProvider;
import io.harness.state.io.StepInputPackageTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(GuiceSuiteRunner.class)
@Suite.SuiteClasses({AmbianceTest.class, AmbianceUtilsTest.class, ChildChainResponseTest.class, InputArgsTest.class,
    InputSetTest.class, PlanTest.class, AdviserRegistryTest.class, OrchestrationEventHandlerRegistryTest.class,
    FacilitatorRegistryTest.class, ResolverRegistryTest.class, StepRegistryTest.class, StepInputPackageTest.class,
    OrchestrationBeansComponentTest.class})
@ModuleProvider(OrchestrationBeansModuleListProvider.class)
public class OrchestrationBeansTestSuite {}
