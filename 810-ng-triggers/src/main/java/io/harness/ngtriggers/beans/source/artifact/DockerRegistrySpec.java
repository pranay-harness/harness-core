package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PIPELINE)
public class DockerRegistrySpec implements ArtifactTypeSpec {
  String connectorRef;
  List<TriggerEventDataCondition> eventConditions;
  String imagePath;
  String tag;

  @Override
  public String fetchConnectorRef() {
    return connectorRef;
  }

  @Override
  public List<TriggerEventDataCondition> fetchEventDataConditions() {
    return eventConditions;
  }
}
