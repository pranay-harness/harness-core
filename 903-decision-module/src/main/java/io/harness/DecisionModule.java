package io.harness;

import io.harness.accesscontrol.DecisionMorphiaRegistrar;
import io.harness.accesscontrol.acl.ACLModule;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.serializer.KryoRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class DecisionModule extends AbstractModule {
  private static DecisionModule instance;
  private DecisionModuleConfiguration decisionModuleConfiguration;

  public DecisionModule(DecisionModuleConfiguration decisionModuleConfiguration) {
    this.decisionModuleConfiguration = decisionModuleConfiguration;
  }

  public static synchronized DecisionModule getInstance(DecisionModuleConfiguration decisionModuleConfiguration) {
    if (instance == null) {
      instance = new DecisionModule(decisionModuleConfiguration);
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
    install(ACLModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleService.class);
    requireBinding(RoleAssignmentService.class);
    requireBinding(ResourceGroupClient.class);
  }
}
