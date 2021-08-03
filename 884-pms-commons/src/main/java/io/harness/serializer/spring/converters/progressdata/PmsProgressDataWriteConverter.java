package io.harness.serializer.spring.converters.progressdata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.progressdata.PmsProgressData;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapAbstractWriteConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class PmsProgressDataWriteConverter extends OrchestrationMapAbstractWriteConverter<PmsProgressData> {
  @Inject
  public PmsProgressDataWriteConverter(KryoSerializer kryoSerializer) {
    super(kryoSerializer);
  }
}
