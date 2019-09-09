package software.wings.service.impl.yaml.handler.trigger;

import software.wings.beans.trigger.TriggerArtifactSelectionFromPipelineSource;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.TriggerArtifactSelectionFromPipelineSourceYaml;

import java.util.List;

public class TriggerArtifactFromSourcePipelineYamlHandler
    extends TriggerArtifactValueYamlHandler<TriggerArtifactSelectionFromPipelineSourceYaml> {
  @Override
  public Class getYamlClass() {
    return TriggerArtifactSelectionFromPipelineSourceYaml.class;
  }

  @Override
  public TriggerArtifactSelectionFromPipelineSourceYaml toYaml(TriggerArtifactSelectionValue bean, String appId) {
    return TriggerArtifactSelectionFromPipelineSourceYaml.builder().build();
  }

  @Override
  public TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<TriggerArtifactSelectionFromPipelineSourceYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    return TriggerArtifactSelectionFromPipelineSource.builder().build();
  }
}
