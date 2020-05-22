package io.harness.yaml.core.defaults;

import io.harness.yaml.core.Tag;
import io.harness.yaml.core.intfc.Stage;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface DefaultPipelineProperties {
  String getName();
  String getDescription();
  List<Tag> getTags();
  @NotNull List<Stage> getStages();
}
