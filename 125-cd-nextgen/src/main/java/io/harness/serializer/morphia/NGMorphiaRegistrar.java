/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.beans.K8sGcpInfraMapping;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.pipeline.executions.CDAccountExecutionMetadata;
import io.harness.cdng.provision.terraform.TerraformConfig;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceConfigOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.polling.bean.PollingDocument;

import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class NGMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InfraMapping.class);
    set.add(K8sDirectInfraMapping.class);
    set.add(K8sGcpInfraMapping.class);
    set.add(DockerArtifactSource.class);
    set.add(ArtifactSource.class);
    set.add(TerraformConfig.class);
    set.add(PollingDocument.class);
    set.add(CDAccountExecutionMetadata.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("cdng.artifact.steps.ArtifactStepParameters", ArtifactStepParameters.class);
    h.put("cdng.service.steps.ServiceStepParameters", ServiceStepParameters.class);
    h.put("cdng.service.ServiceConfig", ServiceConfig.class);
    h.put("cdng.artifact.bean.yaml.ArtifactListConfig", ArtifactListConfig.class);
    h.put("cdng.artifact.bean.yaml.DockerHubArtifactConfig", DockerHubArtifactConfig.class);
    h.put("cdng.artifact.bean.yaml.GcrArtifactConfig", GcrArtifactConfig.class);
    h.put("cdng.artifact.bean.yaml.SidecarArtifact", SidecarArtifact.class);
    h.put("cdng.service.beans.ServiceConfigOutcome", ServiceConfigOutcome.class);
    h.put("cdng.manifest.yaml.ManifestsOutcome", ManifestsOutcome.class);
    h.put("cdng.service.beans.StageOverridesConfig", StageOverridesConfig.class);
    h.put("cdng.service.beans.ServiceUseFromStage", ServiceUseFromStage.class);
    h.put("cdng.service.beans.ServiceUseFromStage$Overrides", Overrides.class);
    h.put("cdng.infra.beans.InfraUseFromStage$Overrides", InfraUseFromStage.Overrides.class);
    h.put("cdng.infra.beans.InfraUseFromStage", InfraUseFromStage.class);
    h.put("cdng.infra.steps.InfraStepParameters", InfraStepParameters.class);
    h.put("io.harness.cdng.provision.terraform.TerraformConfig", TerraformConfig.class);
    h.put("io.harness.polling.bean.PollingDocument", PollingDocument.class);
  }
}
