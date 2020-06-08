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
import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraDefinition;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.service.Service;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class NGMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InfraDefinition.class);
    set.add(InfraMapping.class);
    set.add(K8sDirectInfraDefinition.class);
    set.add(K8sDirectInfraMapping.class);
    set.add(DockerArtifactSource.class);
    set.add(ArtifactSource.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("cdng.artifact.steps.ArtifactStepParameters", ArtifactStepParameters.class);
    h.put("cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes", DockerArtifactSourceAttributes.class);
    h.put("cdng.artifact.bean.yaml.DockerHubArtifactConfig", DockerHubArtifactConfig.class);
    h.put("cdng.artifact.bean.yaml.GcrArtifactConfig", GcrArtifactConfig.class);
    h.put("cdng.service.steps.ServiceStepParameters", ServiceStepParameters.class);
    h.put("cdng.service.Service", Service.class);
    h.put("cdng.artifact.bean.yaml.ArtifactListConfig", ArtifactListConfig.class);
    h.put("cdng.artifact.delegate.task.ArtifactTaskResponse", ArtifactTaskResponse.class);
    h.put("cdng.artifact.bean.DockerArtifactAttributes", DockerArtifactAttributes.class);
    h.put("cdng.artifact.delegate.task.ArtifactTaskParameters", ArtifactTaskParameters.class);
    h.put("cdng.artifact.bean.yaml.SidecarArtifact", SidecarArtifact.class);
    h.put("cdng.artifact.bean.DockerArtifactOutcome", DockerArtifactOutcome.class);
  }
}
