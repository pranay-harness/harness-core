/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.utils.nameservice;

import static io.harness.persistence.HQuery.excludeAuthority;

import static java.util.function.Function.identity;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.utils.nameservice.NameResult.NameResultBuilder;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class NameServiceImpl implements NameService {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public NameResult getNames(@NotNull Set<String> ids, @NotNull String type) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    switch (type) {
      case instanceType:
      case status:
      case artifactType:
      case environmentType:
      case Type:
      case deploymentType:
      case orchestrationWorkflowType:
        ids.remove(null);
        return nameResultBuilder.type(type)
            .idNameMap(ids.stream().collect(Collectors.toMap(identity(), identity())))
            .build();
      default:
        return getData(type, ids);
    }
  }

  private NameResult getData(String type, Set<String> ids) {
    switch (type) {
      case application:
        return getAppNames(ids);
      case service:
        return getServiceNames(ids);
      case environment:
        return getEnvironmentNames(ids);
      case cloudProvider:
        return getCloudProviderNames(ids);
      case triggerId:
        return getTriggerNames(ids);
      case triggeredBy:
        return getUserNames(ids);
      case workflow:
        return getWorkflowNames(ids);
      case pipeline:
        return getPipelineNames(ids);
      default:
        log.warn("Unsupported type :[{}]", type);
        return NameResult.builder().type(type).build();
    }
  }

  private NameResult getAppNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Application> applications =
             new HIterator<>(wingsPersistence.createQuery(Application.class, excludeAuthority)
                                 .project(ApplicationKeys.name, true)
                                 .field(ApplicationKeys.uuid)
                                 .in(ids)
                                 .fetch())) {
      applications.forEachRemaining(application -> { names.put(application.getUuid(), application.getName()); });
    }

    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getServiceNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Service> services = new HIterator<>(wingsPersistence.createQuery(Service.class, excludeAuthority)
                                                           .project(ServiceKeys.name, true)
                                                           .field(ServiceKeys.uuid)
                                                           .in(ids)
                                                           .fetch())) {
      services.forEachRemaining(service -> { names.put(service.getUuid(), service.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getEnvironmentNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Environment> environments =
             new HIterator<>(wingsPersistence.createQuery(Environment.class, excludeAuthority)
                                 .project(EnvironmentKeys.name, true)
                                 .field(EnvironmentKeys.uuid)
                                 .in(ids)
                                 .fetch())) {
      environments.forEachRemaining(environment -> { names.put(environment.getUuid(), environment.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getCloudProviderNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<SettingAttribute> settingAttributes =
             new HIterator<>(wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority)
                                 .field(SettingAttributeKeys.category)
                                 .equal(SettingCategory.CLOUD_PROVIDER)
                                 .project(SettingAttributeKeys.name, true)
                                 .field(SettingAttributeKeys.uuid)
                                 .in(ids)
                                 .fetch())) {
      settingAttributes.forEachRemaining(
          cloudProvider -> { names.put(cloudProvider.getUuid(), cloudProvider.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getTriggerNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Trigger> triggers = new HIterator<>(wingsPersistence.createQuery(Trigger.class, excludeAuthority)
                                                           .project(TriggerKeys.name, true)
                                                           .field(TriggerKeys.uuid)
                                                           .in(ids)
                                                           .fetch())) {
      triggers.forEachRemaining(trigger -> { names.put(trigger.getUuid(), trigger.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getUserNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<User> users = new HIterator<>(wingsPersistence.createQuery(User.class, excludeAuthority)
                                                     .project(UserKeys.name, true)
                                                     .field("_id")
                                                     .in(ids)
                                                     .fetch())) {
      users.forEachRemaining(user -> { names.put(user.getUuid(), user.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getWorkflowNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Workflow> workflows = new HIterator<>(wingsPersistence.createQuery(Workflow.class, excludeAuthority)
                                                             .project(WorkflowKeys.name, true)
                                                             .field(WorkflowKeys.uuid)
                                                             .in(ids)
                                                             .fetch())) {
      workflows.forEachRemaining(workflow -> { names.put(workflow.getUuid(), workflow.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }

  private NameResult getPipelineNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    try (HIterator<Pipeline> pipelines = new HIterator<>(wingsPersistence.createQuery(Pipeline.class, excludeAuthority)
                                                             .project(PipelineKeys.name, true)
                                                             .field(PipelineKeys.uuid)
                                                             .in(ids)
                                                             .fetch())) {
      pipelines.forEachRemaining(pipeline -> { names.put(pipeline.getUuid(), pipeline.getName()); });
    }
    return nameResultBuilder.idNameMap(names).build();
  }
}
