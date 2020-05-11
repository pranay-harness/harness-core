package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.trigger.TriggerArtifactSelectionFromSource;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.TriggerArtifactSelectionFromSourceYaml;

import java.util.List;

@OwnedBy(CDC)
public class TriggerArtifactFromSourceArtifactYamlHandler
    extends TriggerArtifactValueYamlHandler<TriggerArtifactSelectionFromSourceYaml> {
  @Override
  public TriggerArtifactSelectionFromSourceYaml toYaml(TriggerArtifactSelectionValue bean, String appId) {
    return TriggerArtifactSelectionFromSourceYaml.builder().build();
  }

  @Override
  public TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<TriggerArtifactSelectionFromSourceYaml> changeContext, List<ChangeContext> changeSetContext) {
    return TriggerArtifactSelectionFromSource.builder().build();
  }

  @Override
  public Class getYamlClass() {
    return null;
  }
}
