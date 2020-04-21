package io.harness.app.beans.intfc;

import io.harness.beans.CIPipeline;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.Valid;

public interface CIPipelineService {
  @ValidationGroups(Create.class) CIPipeline createWorkflow(@Valid CIPipeline workflow);
}
