package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.NotificationSenderKryoRegistrar;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.serializer.morphia.NotificationSenderMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import org.mongodb.morphia.converters.TypeConverter;

@OwnedBy(PL)
public class NotificationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(NotificationSenderKryoRegistrar.class)
          .add(DelegateTasksBeansKryoRegister.class)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .addAll(ProjectAndOrgRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .addAll(ApiServiceBeansRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(NotificationSenderMorphiaRegistrar.class)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().build();
}
