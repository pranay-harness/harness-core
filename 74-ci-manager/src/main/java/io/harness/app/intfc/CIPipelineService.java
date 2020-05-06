package io.harness.app.intfc;

import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.Valid;

public interface CIPipelineService {
  @ValidationGroups(Create.class) CIPipeline createPipeline(@Valid CIPipeline ciPipeline);

  CIPipeline readPipeline(String pipelineId);

  CIPipeline createPipelineFromYAML(YAML yaml);
}
