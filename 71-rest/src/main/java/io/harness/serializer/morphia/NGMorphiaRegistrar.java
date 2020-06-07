package io.harness.serializer.morphia;

import io.harness.cdng.artifact.bean.Artifact;
import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifact;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.state.ArtifactStepParameters;
import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraDefinition;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
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
    set.add(Artifact.class);
    set.add(DockerArtifact.class);
    set.add(ArtifactSource.class);
    set.add(DockerArtifactAttributes.class);
    set.add(ArtifactAttributes.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("cdng.artifact.state.ArtifactStepParameters", ArtifactStepParameters.class);
    h.put("cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes", DockerArtifactSourceAttributes.class);
    h.put("cdng.artifact.bean.yaml.DockerHubArtifactConfig", DockerHubArtifactConfig.class);
  }
}
