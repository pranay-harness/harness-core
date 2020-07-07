package io.harness.serializer.morphia;

import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.environment.beans.Environment;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.StageOverridesConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class NGMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InfraMapping.class);
    set.add(K8sDirectInfraMapping.class);
    set.add(DockerArtifactSource.class);
    set.add(ArtifactSource.class);
    set.add(Environment.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("cdng.artifact.steps.ArtifactStepParameters", ArtifactStepParameters.class);
    h.put("cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes", DockerArtifactSourceAttributes.class);
    h.put("cdng.artifact.bean.yaml.DockerHubArtifactConfig", DockerHubArtifactConfig.class);
    h.put("cdng.artifact.bean.yaml.GcrArtifactConfig", GcrArtifactConfig.class);
    h.put("cdng.service.steps.ServiceStepParameters", ServiceStepParameters.class);
    h.put("cdng.service.ServiceConfig", ServiceConfig.class);
    h.put("cdng.artifact.bean.yaml.ArtifactListConfig", ArtifactListConfig.class);
    h.put("cdng.artifact.delegate.task.ArtifactTaskResponse", ArtifactTaskResponse.class);
    h.put("cdng.artifact.bean.DockerArtifactAttributes", DockerArtifactAttributes.class);
    h.put("cdng.artifact.delegate.task.ArtifactTaskParameters", ArtifactTaskParameters.class);
    h.put("cdng.artifact.bean.yaml.SidecarArtifact", SidecarArtifact.class);
    h.put("cdng.artifact.bean.DockerArtifactOutcome", DockerArtifactOutcome.class);
    h.put("cdng.service.beans.ServiceOutcome", ServiceOutcome.class);
    h.put("cdng.service.beans.ServiceOutcome$ArtifactsOutcome", ArtifactsOutcome.class);
    h.put("cdng.environment.beans.Environment", Environment.class);
    h.put("cdng.service.beans.StageOverridesConfig", StageOverridesConfig.class);
    h.put("cdng.service.beans.ServiceUseFromStage", ServiceUseFromStage.class);
    h.put("cdng.service.beans.ServiceUseFromStage$Overrides", Overrides.class);
    h.put("cdng.infra.beans.InfraUseFromStage$Overrides", InfraUseFromStage.Overrides.class);
    h.put("cdng.infra.beans.InfraUseFromStage", InfraUseFromStage.class);
    h.put("cdng.environment.steps.EnvironmentStepParameters", EnvironmentStepParameters.class);
    h.put("cdng.infra.steps.InfraStepParameters", InfraStepParameters.class);
  }
}
