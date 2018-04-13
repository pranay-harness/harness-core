package software.wings.generator;

import static software.wings.beans.Pipeline.Builder.aPipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.Builder;
import software.wings.service.intfc.PipelineService;

@Singleton
public class PipelineGenerator {
  @Inject PipelineService pipelineService;

  public Pipeline createPipeline(Randomizer.Seed seed, Pipeline pipeline) {
    EnhancedRandom random = Randomizer.instance(seed);

    final Builder builder = aPipeline();

    if (pipeline != null && pipeline.getAppId() != null) {
      builder.withAppId(pipeline.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (pipeline != null && pipeline.getName() != null) {
      builder.withName(pipeline.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (pipeline != null && pipeline.getPipelineStages() != null) {
      builder.withPipelineStages(pipeline.getPipelineStages());
    } else {
      throw new UnsupportedOperationException();
    }

    return pipelineService.createPipeline(builder.build());
  }
}
