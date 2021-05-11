package io.harness.serializer;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.CVStepInfoBase;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.pms.serializer.kryo.PmsContractsKryoRegistrar;
import io.harness.serializer.kryo.CVNGKryoRegistrar;
import io.harness.serializer.morphia.CVNextGenMorphiaRegister;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
@OwnedBy(HarnessTeam.CV)
public class CvNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .add(CVNGKryoRegistrar.class)
          .add(PipelineServiceUtilKryoRegistrar.class)
          .add(PmsContractsKryoRegistrar.class)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.morphiaRegistrars)
          .add(CVNextGenMorphiaRegister.class)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(PersistenceRegistrars.morphiaConverters)
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .build();
  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STEPS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CVStepInfoBase.class)
                   .build())
          .build();
}
