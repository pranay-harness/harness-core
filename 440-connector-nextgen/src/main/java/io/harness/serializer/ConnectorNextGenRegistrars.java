package io.harness.serializer;

import io.harness.EntityType;
import io.harness.connector.ConnectorDTO;
import io.harness.filter.serializer.FiltersRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.ConnectorNextGenKryoRegistrar;
import io.harness.serializer.morphia.ConnectorMorphiaClassesRegistrar;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class ConnectorNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .addAll(NGCoreRegistrars.kryoRegistrars)
          .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
          .addAll(NGCoreClientRegistrars.kryoRegistrars)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .addAll(DelegateAgentBeansRegistrars.kryoRegistrars)
          .add(ConnectorNextGenKryoRegistrar.class)
          .addAll(PmsCommonsModuleRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCoreRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
          .addAll(NGCoreClientRegistrars.morphiaRegistrars)
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .addAll(FiltersRegistrars.morphiaRegistrars)
          .addAll(SMCoreRegistrars.morphiaRegistrars)
          .add(ConnectorMorphiaClassesRegistrar.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .add(PmsSdkCoreMorphiaRegistrar.class)
          .addAll(PmsCommonsModuleRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.CONNECTORS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(true)
                   .availableAtAccountLevel(true)
                   .clazz(ConnectorDTO.class)
                   .build())
          .build();
}
