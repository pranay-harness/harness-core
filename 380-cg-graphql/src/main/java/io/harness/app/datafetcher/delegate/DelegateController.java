package io.harness.app.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.delegate.input.QLAddDelegateScopeInput;
import io.harness.app.schema.type.delegate.QLDelegate;
import io.harness.app.schema.type.delegate.QLDelegateScope;
import io.harness.app.schema.type.delegate.QLTaskGroup;
import io.harness.beans.EnvironmentType;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.TaskGroup;

import software.wings.graphql.schema.type.QLEnvironmentType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DEL)
public class DelegateController {
  private static final Map<String, TaskGroup> taskGroupMapping = taskGroupMapping();

  public static void populateQLDelegate(Delegate delegate, QLDelegate.QLDelegateBuilder qlDelegateBuilder) {
    qlDelegateBuilder.accountId(delegate.getAccountId())
        .delegateName(delegate.getDelegateName())
        .delegateType(delegate.getDelegateType())
        .description(delegate.getDescription())
        .hostName(delegate.getHostName())
        .pollingModeEnabled(delegate.isPolllingModeEnabled())
        .ip(delegate.getIp())
        .status(delegate.getStatus().toString())
        .delegateProfileId(delegate.getDelegateProfileId())
        .lastHeartBeat(delegate.getLastHeartBeat())
        .build();
  }

  public static List<QLDelegate> populateQLDelegateList(List<Delegate> delegateList) {
    List<QLDelegate> qlDelegateList = new ArrayList<>();
    for (Delegate delegate : delegateList) {
      QLDelegate.QLDelegateBuilder qlDelegateBuilder = QLDelegate.builder();
      populateQLDelegate(delegate, qlDelegateBuilder);
      qlDelegateList.add(qlDelegateBuilder.build());
    }
    return qlDelegateList;
  }

  public static Map<String, TaskGroup> taskGroupMapping() {
    Map<String, TaskGroup> taskGroupTaskMap = new HashMap<>();
    Set<TaskGroup> taskGroupSet = EnumSet.allOf(TaskGroup.class);
    taskGroupSet.forEach(taskGroup -> taskGroupTaskMap.put(taskGroup.name(), taskGroup));
    return taskGroupTaskMap;
  }

  public static void populateDelegateScope(String accountId, QLAddDelegateScopeInput delegateScopeInput,
      DelegateScope.DelegateScopeBuilder delegateScopeBuilder) {
    List<TaskGroup> taskGroupList = new ArrayList<>();
    if (delegateScopeInput.getTaskGroups() != null && !delegateScopeInput.getTaskGroups().isEmpty()) {
      taskGroupList = delegateScopeInput.getTaskGroups()
                          .stream()
                          .filter(Objects::nonNull)
                          .map(taskGroup -> taskGroupMapping().get(taskGroup.name()))
                          .collect(Collectors.toList());
    }
    delegateScopeBuilder.name(delegateScopeInput.getName())
        .accountId(accountId)
        .taskTypes(taskGroupList)
        .environments(delegateScopeInput.getEnvironments())
        .environmentTypes(populateEnvironmentTypeList(delegateScopeInput.getEnvironmentTypes()))
        .services(delegateScopeInput.getServices())
        .applications(delegateScopeInput.getApplications())
        .build();
  }

  public static void populateQLDelegateScope(
      DelegateScope delegateScope, QLDelegateScope.QLDelegateScopeBuilder qlDelegateScopeBuilder) {
    qlDelegateScopeBuilder.name(delegateScope.getName())
        .applications(delegateScope.getApplications())
        .services(delegateScope.getApplications())
        .environments(delegateScope.getEnvironments())
        .environmentTypes(populateQLEnvironmentTypeList(delegateScope.getEnvironmentTypes()))
        .build();
  }

  public static List<TaskGroup> populateTaskGroup(List<QLTaskGroup> qlTaskGroup) {
    List<TaskGroup> taskGroupList = qlTaskGroup.stream()
                                        .filter(Objects::nonNull)
                                        .map(taskGroup -> taskGroupMapping().get(taskGroup.name()))
                                        .collect(Collectors.toList());
    return taskGroupList;
  }

  public static List<EnvironmentType> populateEnvironmentTypeList(List<QLEnvironmentType> qlEnvironmentTypeList) {
    List<EnvironmentType> environmentTypeList = new ArrayList<>();
    if (qlEnvironmentTypeList == null || qlEnvironmentTypeList.isEmpty()) {
      return environmentTypeList;
    }
    environmentTypeList = qlEnvironmentTypeList.stream()
                              .filter(Objects::nonNull)
                              .map(DelegateController::toEnvironmentType)
                              .collect(Collectors.toList());
    return environmentTypeList;
  }

  public static List<QLEnvironmentType> populateQLEnvironmentTypeList(List<EnvironmentType> environmentTypeList) {
    List<QLEnvironmentType> qlEnvironmentTypeList = new ArrayList<>();
    if (environmentTypeList.isEmpty()) {
      return qlEnvironmentTypeList;
    }
    qlEnvironmentTypeList = environmentTypeList.stream()
                                .filter(Objects::nonNull)
                                .map(DelegateController::toQLEnvironmentType)
                                .collect(Collectors.toList());
    return qlEnvironmentTypeList;
  }

  private EnvironmentType toEnvironmentType(QLEnvironmentType qlEnvironmentType) {
    if (qlEnvironmentType == null) {
      return null;
    }
    if (qlEnvironmentType == QLEnvironmentType.PROD) {
      return EnvironmentType.PROD;
    }
    if (qlEnvironmentType == QLEnvironmentType.NON_PROD) {
      return EnvironmentType.NON_PROD;
    }
    return EnvironmentType.ALL;
  }

  private QLEnvironmentType toQLEnvironmentType(EnvironmentType environmentType) {
    if (environmentType == null) {
      return null;
    }
    if (environmentType == EnvironmentType.PROD) {
      return QLEnvironmentType.PROD;
    }
    if (environmentType == EnvironmentType.NON_PROD) {
      return QLEnvironmentType.NON_PROD;
    }
    return QLEnvironmentType.ALL;
  }
}
