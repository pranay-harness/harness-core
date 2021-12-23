package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.helm.HelmSubCommandType;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Getter;

@Getter
public enum HelmCommandFlagType {
  Fetch(HelmSubCommandType.FETCH, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES),
      ManifestStoreType.HelmChartRepo),
  Template(HelmSubCommandType.TEMPLATE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES),
      ManifestStoreType.HelmAllRepo),
  Pull(HelmSubCommandType.PULL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES),
      ManifestStoreType.HelmChartRepo),
  Install(HelmSubCommandType.INSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  Upgrade(HelmSubCommandType.UPGRADE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  Rollback(HelmSubCommandType.ROLLBACK, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  History(HelmSubCommandType.HISTORY, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  Delete(HelmSubCommandType.DELETE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  Uninstall(HelmSubCommandType.UNINSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo),
  List(HelmSubCommandType.LIST, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmAllRepo);

  private final HelmSubCommandType subCommandType;
  private final Set<String> serviceSpecTypes;
  private final Set<String> storeTypes;
  HelmCommandFlagType(HelmSubCommandType subCommandType, Set<String> serviceSpecTypes, Set<String> storeTypes) {
    this.subCommandType = subCommandType;
    this.serviceSpecTypes = serviceSpecTypes;
    this.storeTypes = storeTypes;
  }
}
