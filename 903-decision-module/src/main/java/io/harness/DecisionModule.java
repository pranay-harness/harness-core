package io.harness;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;

import io.harness.accesscontrol.DecisionMorphiaRegistrar;
import io.harness.accesscontrol.acl.ACLModule;
import io.harness.aggregator.AggregatorModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.resourcegroupclient.ResourceGroupClientModule;
import io.harness.serializer.KryoRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class DecisionModule extends AbstractModule {
  private static DecisionModule instance;
  private final DecisionModuleConfiguration configuration;

  public DecisionModule(DecisionModuleConfiguration configuration) {
    this.configuration = configuration;
  }

  public static synchronized DecisionModule getInstance(DecisionModuleConfiguration configuration) {
    if (instance == null) {
      instance = new DecisionModule(configuration);
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends KryoRegistrar>> kryoRegistrar =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends KryoRegistrar>>() {});
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(DecisionMorphiaRegistrar.class);
    install(new ResourceGroupClientModule(configuration.getResourceGroupServiceConfig(),
        configuration.getResourceGroupServiceSecret(), ACCESS_CONTROL_SERVICE.getServiceId()));
    install(AggregatorModule.getInstance());
    install(ACLModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
