package io.harness.serializer;

import io.harness.ccm.serializer.morphia.CECommonsMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentBeansKryoRegister;
import io.harness.serializer.kryo.DelegateAgentKryoRegister;
import io.harness.serializer.kryo.DelegateServiceKryoRegister;
import io.harness.serializer.kryo.ManagerKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationStepsKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationVisualizationKryoRegistrar;
import io.harness.serializer.kryo.ProjectAndOrgKryoRegistrar;
import io.harness.serializer.morphia.CommonEntitiesMorphiaRegister;
import io.harness.serializer.morphia.DelegateServiceBeansMorphiaRegistrar;
import io.harness.serializer.morphia.DelegateServiceMorphiaRegistrar;
import io.harness.serializer.morphia.EventMorphiaRegistrar;
import io.harness.serializer.morphia.InvitesMorphiaRegistrar;
import io.harness.serializer.morphia.LimitsMorphiaRegistrar;
import io.harness.serializer.morphia.ManagerMorphiaRegistrar;
import io.harness.serializer.morphia.OrchestrationStepsMorphiaRegistrar;
import io.harness.serializer.morphia.ProjectAndOrgMorphiaRegistrar;
import io.harness.serializer.morphia.ViewsMorphiaRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class ManagerRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationStepsKryoRegistrar.class)
          .add(OrchestrationVisualizationKryoRegistrar.class)
          .add(ManagerKryoRegistrar.class)
          .add(ProjectAndOrgKryoRegistrar.class)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(NGCoreRegistrars.kryoRegistrars)
          .addAll(RbacCoreRegistrars.kryoRegistrars)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .addAll(NotificationSenderRegistrars.kryoRegistrars)
          .add(CvNextGenCommonsBeansKryoRegistrar.class)
          // temporary:
          .add(DelegateAgentKryoRegister.class)
          .add(DelegateAgentBeansKryoRegister.class)
          .add(DelegateServiceKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(FeatureFlagRegistrars.morphiaRegistrars)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(NGCoreRegistrars.morphiaRegistrars)
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .addAll(RbacCoreRegistrars.morphiaRegistrars)
          .addAll(SMCoreRegistrars.morphiaRegistrars)
          .add(CECommonsMorphiaRegistrar.class)
          .add(CommonEntitiesMorphiaRegister.class)
          .add(DelegateServiceBeansMorphiaRegistrar.class)
          .add(DelegateServiceMorphiaRegistrar.class)
          .add(EventMorphiaRegistrar.class)
          .add(InvitesMorphiaRegistrar.class)
          .add(LimitsMorphiaRegistrar.class)
          .add(ManagerMorphiaRegistrar.class)
          .add(OrchestrationStepsMorphiaRegistrar.class)
          .add(ProjectAndOrgMorphiaRegistrar.class)
          .add(ViewsMorphiaRegistrar.class)
          .addAll(NotificationSenderRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(PersistenceRegistrars.morphiaConverters)
          .addAll(OrchestrationRegistrars.morphiaConverters)
          .addAll(DelegateTasksBeansRegistrars.morphiaConverters)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .addAll(OrchestrationRegistrars.springConverters)
          .build();
}
