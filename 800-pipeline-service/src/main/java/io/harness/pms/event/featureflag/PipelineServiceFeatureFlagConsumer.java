package io.harness.pms.event.featureflag;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PIPELINE_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class PipelineServiceFeatureFlagConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public PipelineServiceFeatureFlagConsumer(@Named(FEATURE_FLAG_STREAM) Consumer redisConsumer,
      @Named(PIPELINE_ENTITY + FEATURE_FLAG_STREAM) MessageListener pipelineFeatureFlagListener) {
    super(redisConsumer, pipelineFeatureFlagListener, true);
  }
}
