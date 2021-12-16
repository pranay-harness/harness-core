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
      ManifestStoreType.HelmNGRepo),
  Pull(HelmSubCommandType.PULL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM, ServiceSpecType.KUBERNETES),
      ManifestStoreType.HelmChartRepo),
  Install(HelmSubCommandType.INSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  Upgrade(HelmSubCommandType.UPGRADE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  Rollback(HelmSubCommandType.ROLLBACK, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  History(HelmSubCommandType.HISTORY, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  Delete(HelmSubCommandType.DELETE, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  Uninstall(HelmSubCommandType.UNINSTALL, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo),
  List(HelmSubCommandType.LIST, ImmutableSet.of(ServiceSpecType.NATIVE_HELM), ManifestStoreType.HelmNGRepo);

  private final HelmSubCommandType subCommandType;
  private final Set<String> serviceSpecTypes;
  private final Set<String> storeTypes;
  HelmCommandFlagType(HelmSubCommandType subCommandType, Set<String> serviceSpecTypes, Set<String> storeTypes) {
    this.subCommandType = subCommandType;
    this.serviceSpecTypes = serviceSpecTypes;
    this.storeTypes = storeTypes;
  }
}
