package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.RbacCoreKryoRegistrar;
import io.harness.serializer.morphia.DelegateTasksBeansMorphiaRegistrar;
import io.harness.serializer.morphia.converters.CapabilityParametersMorphiaConverter;
import io.harness.serializer.morphia.converters.TestingCapabilityMorphiaConverter;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class DelegateTasksBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ApiServiceBeansRegistrars.kryoRegistrars)
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(ScmJavaClientRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .add(RbacCoreKryoRegistrar.class)
          .add(DelegateTasksBeansKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ApiServiceBeansRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(DelegateTasksBeansMorphiaRegistrar.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .add(CapabilityParametersMorphiaConverter.class)
          .add(TestingCapabilityMorphiaConverter.class)
          .build();
}
