package io.harness.pms.ngpipeline.inputset.observers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.observer.PipelineActionObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class InputSetsDeleteObserver implements PipelineActionObserver {
  @Inject PMSInputSetService pmsInputSetService;

  @Override
  public void onDelete(PipelineEntity pipelineEntity) {
    pmsInputSetService.deleteInputSetsOnPipelineDeletion(pipelineEntity);
    log.info("All inputSets of pipeline {} deleted", pipelineEntity.getIdentifier());
  }
}
