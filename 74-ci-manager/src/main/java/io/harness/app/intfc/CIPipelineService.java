package io.harness.app.intfc;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.yaml.YAML;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import java.util.List;
import javax.validation.Valid;

public interface CIPipelineService {
  @ValidationGroups(Create.class) CDPipelineEntity createPipeline(@Valid CDPipelineEntity ciPipeline);

  CDPipelineEntity readPipeline(String pipelineId, String accountId, String orgId, String projectId);

  CDPipelineEntity readPipeline(String pipelineId);

  CDPipelineEntity createPipelineFromYAML(YAML yaml, String accountId, String orgId, String projectId);

  List<CDPipelineEntity> getPipelines(CIPipelineFilterDTO ciPipelineFilterDTO);
}
