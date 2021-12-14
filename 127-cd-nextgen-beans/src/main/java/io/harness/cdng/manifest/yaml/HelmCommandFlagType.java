package io.harness.cdng.manifest.yaml;

import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.helm.HelmSubCommandType;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Getter;

@Getter
public enum HelmCommandFlagType {
  Fetch(HelmSubCommandType.FETCH, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES)),
  Version(HelmSubCommandType.VERSION, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES)),
  Template(HelmSubCommandType.TEMPLATE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES)),
  Pull(HelmSubCommandType.PULL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES)),
  Install(HelmSubCommandType.INSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  Upgrade(HelmSubCommandType.UPGRADE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  Rollback(HelmSubCommandType.ROLLBACK, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  History(HelmSubCommandType.HISTORY, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  Delete(HelmSubCommandType.DELETE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  Uninstall(HelmSubCommandType.UNINSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM)),
  List(HelmSubCommandType.LIST, ImmutableSet.of(ServiceSpecType.NATIVE_HELM));

  private final HelmSubCommandType subCommandType;
  private final Set<String> serviceSpecTypes;

  HelmCommandFlagType(HelmSubCommandType subCommandType, Set<String> serviceSpecTypes) {
    this.subCommandType = subCommandType;
    this.serviceSpecTypes = serviceSpecTypes;
  }
}
