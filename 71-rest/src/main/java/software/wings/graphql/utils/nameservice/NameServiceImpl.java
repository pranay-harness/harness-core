package software.wings.graphql.utils.nameservice;

import com.google.inject.Inject;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NameServiceImpl implements NameService {
  @Inject WingsPersistence wingsPersistence;

  private static final String application = "Application";
  private static final String service = "Service";
  private static final String environment = "Environment";
  private static final String cloudProvider = "CloudProvider";
  private static final String status = "Status";
  private static final String triggeredBy = "TriggeredBy";
  private static final String triggerId = "TriggerId";
  private static final String workflow = "Workflow";
  private static final String pipeline = "Pipeline";
  private static final String instanceType = "InstanceType";

  @Override
  public NameResult getNames(@NotNull Set<String> ids, @NotNull String type) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    switch (type) {
      case instanceType:
      case status:
        return nameResultBuilder.type(type).nameMap(ids.stream().collect(Collectors.toMap(id -> id, id -> id))).build();
      default:
        return getData(type, ids);
    }
  }

  private NameResult getData(String type, Set<String> ids) {
    Query query;
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
      default:
        logger.warn("Unsupported type :[{}]", type);
        return NameResult.builder().type(type).notFoundNames(ids).build();
    }
  }

  private NameResult getAppNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(Application.class)
        .project(ApplicationKeys.name, true)
        .field(ApplicationKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(application -> {
          names.put(application.getUuid(), application.getName());
          ids.remove(application.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getServiceNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(Service.class)
        .project(ServiceKeys.name, true)
        .field(ServiceKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(service -> {
          names.put(service.getUuid(), service.getName());
          ids.remove(service.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getEnvironmentNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(Environment.class)
        .project(EnvironmentKeys.name, true)
        .field(EnvironmentKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(environment -> {
          names.put(environment.getUuid(), environment.getName());
          ids.remove(environment.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getCloudProviderNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(SettingAttribute.class)
        .field(SettingAttributeKeys.category)
        .equal(SettingCategory.CLOUD_PROVIDER)
        .project(SettingAttributeKeys.name, true)
        .field(SettingAttributeKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(cloudProvider -> {
          names.put(cloudProvider.getUuid(), cloudProvider.getName());
          ids.remove(cloudProvider.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getTriggerNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(Trigger.class)
        .project(TriggerKeys.name, true)
        .field(TriggerKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(trigger -> {
          names.put(trigger.getUuid(), trigger.getName());
          ids.remove(trigger.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getUserNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(User.class)
        .project(UserKeys.name, true)
        .field("_id")
        .in(ids)
        .fetch()
        .forEachRemaining(user -> {
          names.put(user.getUuid(), user.getName());
          ids.remove(user.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }

  private NameResult getWorkflowNames(Set<String> ids) {
    NameResultBuilder nameResultBuilder = NameResult.builder();
    Map<String, String> names = new HashMap<>();
    wingsPersistence.createQuery(Workflow.class)
        .project(WorkflowKeys.name, true)
        .field(WorkflowKeys.uuid)
        .in(ids)
        .fetch()
        .forEachRemaining(user -> {
          names.put(user.getUuid(), user.getName());
          ids.remove(user.getUuid());
        });
    return nameResultBuilder.nameMap(names).notFoundNames(ids).build();
  }
}
