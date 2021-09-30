package io.harness.serializer.jackson.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.data.stepparameters.PmsSecretSanitizer;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

@OwnedBy(PIPELINE)
public class OrchestrationMapSerializer extends JsonSerializer<OrchestrationMap> {
  @Override
  public void serialize(OrchestrationMap orchestrationMap, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeRawValue(PmsSecretSanitizer.sanitize(RecastOrchestrationUtils.toSimpleJson(orchestrationMap)));
  }
}
