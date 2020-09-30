package io.harness.app.intfc;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.yaml.YAML;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import java.util.List;
import javax.validation.Valid;

public interface CIPipelineService {
  @ValidationGroups(Create.class) NgPipelineEntity createPipeline(@Valid NgPipelineEntity ngPipelineEntity);

  NgPipelineEntity readPipeline(String pipelineId, String accountId, String orgId, String projectId);

  NgPipelineEntity readPipeline(String pipelineId);

  NgPipelineEntity createPipelineFromYAML(YAML yaml, String accountId, String orgId, String projectId);

  List<NgPipelineEntity> getPipelines(CIPipelineFilterDTO ciPipelineFilterDTO);
}
