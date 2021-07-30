package io.harness.serializer.spring.converters.stepparameters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.stepparameters.PmsStepParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapAbstractWriteConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class PmsStepParametersWriteConverter extends OrchestrationMapAbstractWriteConverter<PmsStepParameters> {
  @Inject
  public PmsStepParametersWriteConverter(KryoSerializer kryoSerializer) {
    super(kryoSerializer);
  }
}
