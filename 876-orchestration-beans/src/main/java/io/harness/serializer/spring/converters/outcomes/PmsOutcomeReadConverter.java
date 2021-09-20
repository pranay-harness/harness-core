package io.harness.serializer.spring.converters.outcomes;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.PmsOutcome;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapAbstractReadConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class PmsOutcomeReadConverter extends OrchestrationMapAbstractReadConverter<PmsOutcome> {
  @Inject
  public PmsOutcomeReadConverter(KryoSerializer kryoSerializer) {
    super(kryoSerializer);
  }
}
