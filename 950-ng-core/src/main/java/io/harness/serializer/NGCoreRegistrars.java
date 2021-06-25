package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import io.harness.serializer.morphia.NGCoreMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class NGCoreRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .addAll(ScmJavaClientRegistrars.kryoRegistrars)
          .add(NGCoreKryoRegistrar.class)
          .addAll(NGAuditCommonsRegistrars.kryoRegistrars)
          .addAll(OutboxEventRegistrars.kryoRegistrars)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.morphiaRegistrars)
          .addAll(SetupUsageRegistrars.morphiaRegistrars)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(ScmJavaClientRegistrars.morphiaRegistrars)
          .add(NGCoreMorphiaClassesRegistrar.class)
          .addAll(NGAuditCommonsRegistrars.morphiaRegistrars)
          .addAll(OutboxEventRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
}
