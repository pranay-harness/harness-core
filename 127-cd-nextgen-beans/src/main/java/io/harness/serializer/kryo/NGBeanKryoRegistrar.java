/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.instance.outcome.DeploymentInfoOutcome;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmCommandFlagType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizePatchesManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.CDP)
public class NGBeanKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(K8sDirectInfrastructureOutcome.class, 8105);
    kryo.register(K8sGcpInfrastructureOutcome.class, 8300);
    kryo.register(DeploymentInfoOutcome.class, 12547);
    kryo.register(HelmChartManifestOutcome.class, 12524);
    kryo.register(HelmCommandFlagType.class, 12526);
    kryo.register(HelmManifestCommandFlag.class, 12525);
    kryo.register(K8sManifestOutcome.class, 12502);
    kryo.register(KustomizeManifestOutcome.class, 12532);
    kryo.register(OpenshiftManifestOutcome.class, 12535);
    kryo.register(OpenshiftParamManifestOutcome.class, 12537);
    kryo.register(ValuesManifestOutcome.class, 12503);
    kryo.register(ServiceOutcome.class, 8018);
    kryo.register(ServiceOutcome.ArtifactsOutcome.class, 8019);
    kryo.register(ServiceOutcome.StageOverridesOutcome.class, 12504);
    kryo.register(ServiceOutcome.ArtifactsWrapperOutcome.class, 12505);
    kryo.register(ServiceOutcome.ManifestsWrapperOutcome.class, 12506);
    kryo.register(ServiceOutcome.VariablesWrapperOutcome.class, 12507);
    kryo.register(StoreConfig.class, 8022);
    kryo.register(KustomizePatchesManifestOutcome.class, 12548);
  }
}
