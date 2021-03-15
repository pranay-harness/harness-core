package io.harness.serializer;

import io.harness.EntityType;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.serializer.morphia.ResourceGroupSerializer;
import io.harness.serializer.morphia.UserGroupMorphiaRegistrar;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class NextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(CDNGRegistrars.kryoRegistrars)
          .addAll(OutboxEventRegistrars.kryoRegistrars)
          .addAll(NGAuditServiceRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.morphiaRegistrars)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(CDNGRegistrars.morphiaRegistrars)
          .add(UserGroupMorphiaRegistrar.class)
          .addAll(ResourceGroupSerializer.morphiaRegistrars)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(OutboxEventRegistrars.morphiaRegistrars)
          .addAll(NGAuditServiceRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .addAll(ConnectorNextGenRegistrars.yamlSchemaRegistrars)
          .addAll(DelegateServiceBeansRegistrars.yamlSchemaRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.yamlSchemaRegistrars)
          .addAll(CDNGRegistrars.yamlSchemaRegistrars)
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.SECRETS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(true)
                   .availableAtAccountLevel(true)
                   .clazz(SecretRequestWrapper.class)
                   .build())
          .build();
}
