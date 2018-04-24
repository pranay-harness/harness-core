package software.wings.generator;

import static software.wings.beans.Pipeline.PipelineBuilder;
import static software.wings.beans.Pipeline.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Pipeline;
import software.wings.service.intfc.PipelineService;

@Singleton
public class PipelineGenerator {
  @Inject PipelineService pipelineService;

  public Pipeline createPipeline(Randomizer.Seed seed, Pipeline pipeline) {
    EnhancedRandom random = Randomizer.instance(seed);

    final PipelineBuilder builder = builder();

    if (pipeline != null && pipeline.getAppId() != null) {
      builder.appId(pipeline.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (pipeline != null && pipeline.getName() != null) {
      builder.name(pipeline.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (pipeline != null && pipeline.getPipelineStages() != null) {
      builder.pipelineStages(pipeline.getPipelineStages());
    } else {
      throw new UnsupportedOperationException();
    }

    return pipelineService.save(builder.build());
  }
}
