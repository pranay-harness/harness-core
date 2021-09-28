package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.DOCKER_REGISTRY;
import static io.harness.ngtriggers.Constants.ECR;
import static io.harness.ngtriggers.Constants.GCR;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GcrSpec.class, name = GCR)
  , @JsonSubTypes.Type(value = EcrSpec.class, name = ECR),
      @JsonSubTypes.Type(value = DockerRegistrySpec.class, name = DOCKER_REGISTRY)
})

@OwnedBy(PIPELINE)
public interface ArtifactTypeSpec {
  String fetchConnectorRef();
  String fetchBuildType();
  List<TriggerEventDataCondition> fetchEventDataConditions();
}
