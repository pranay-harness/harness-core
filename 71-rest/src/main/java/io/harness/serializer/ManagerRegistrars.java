package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentBeansKryoRegister;
import io.harness.serializer.kryo.DelegateAgentKryoRegister;
import io.harness.serializer.kryo.DelegateServiceKryoRegister;
import io.harness.serializer.kryo.ManagerKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationStepsKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationVisualizationKryoRegistrar;
import io.harness.serializer.kryo.ProjectAndOrgKryoRegistrar;
import io.harness.serializer.kryo.SecretManagerClientKryoRegistrar;
import io.harness.serializer.morphia.CommonEntitiesMorphiaRegister;
import io.harness.serializer.morphia.ConnectorMorphiaClassesRegistrar;
import io.harness.serializer.morphia.DelegateServiceBeansMorphiaRegistrar;
import io.harness.serializer.morphia.DelegateServiceMorphiaRegistrar;
import io.harness.serializer.morphia.EventMorphiaRegistrar;
import io.harness.serializer.morphia.InvitesMorphiaRegistrar;
import io.harness.serializer.morphia.LimitsMorphiaRegistrar;
import io.harness.serializer.morphia.ManagerMorphiaRegistrar;
import io.harness.serializer.morphia.NGCoreMorphiaClassesRegistrar;
import io.harness.serializer.morphia.OrchestrationStepsMorphiaRegistrar;
import io.harness.serializer.morphia.ProjectAndOrgMorphiaRegistrar;
import io.harness.serializer.morphia.SMCoreMorphiaRegistrar;
import io.harness.serializer.morphia.SecretManagerClientMorphiaRegistrar;
import io.harness.serializer.morphia.ViewsMorphiaRegistrar;
import io.harness.serializer.spring.WingsAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ManagerRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationStepsKryoRegistrar.class)
          .add(OrchestrationVisualizationKryoRegistrar.class)
          .add(ManagerKryoRegistrar.class)
          .add(ProjectAndOrgKryoRegistrar.class)
          .addAll(NGCoreRegistrars.kryoRegistrars)
          .add(SecretManagerClientKryoRegistrar.class)
          .add(CvNextGenCommonsBeansKryoRegistrar.class)
          // temporary:
          .add(DelegateAgentKryoRegister.class)
          .add(DelegateAgentBeansKryoRegister.class)
          .add(DelegateServiceKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .add(OrchestrationStepsMorphiaRegistrar.class)
          .add(ManagerMorphiaRegistrar.class)
          .add(LimitsMorphiaRegistrar.class)
          .add(ProjectAndOrgMorphiaRegistrar.class)
          .add(InvitesMorphiaRegistrar.class)
          .add(CommonEntitiesMorphiaRegister.class)
          .addAll(NGCoreRegistrars.morphiaRegistrars)
          .add(SecretManagerClientMorphiaRegistrar.class)
          .add(DelegateServiceBeansMorphiaRegistrar.class)
          .add(NGCoreMorphiaClassesRegistrar.class)
          .add(ConnectorMorphiaClassesRegistrar.class)
          .add(SMCoreMorphiaRegistrar.class)
          .add(EventMorphiaRegistrar.class)
          .add(DelegateServiceMorphiaRegistrar.class)
          .add(ViewsMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.aliasRegistrars)
          .addAll(PersistenceRegistrars.aliasRegistrars)
          .addAll(TimeoutEngineRegistrars.aliasRegistrars)
          .addAll(OrchestrationBeansRegistrars.aliasRegistrars)
          .addAll(OrchestrationRegistrars.aliasRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.aliasRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.aliasRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.aliasRegistrars)
          .addAll(YamlBeansModuleRegistrars.aliasRegistrars)
          .addAll(ExecutionPlanModuleRegistrars.aliasRegistrars)
          .add(WingsAliasRegistrar.class)
          .build();
}
