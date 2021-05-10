package io.harness.serializer.jackson.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.bson.Document;

@OwnedBy(PIPELINE)
public class RecastDocumentSerializer extends JsonSerializer<Document> {
  @Override
  public void serialize(Document document, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    jsonGenerator.writeRawValue(RecastOrchestrationUtils.toSimpleJson(document));
  }
}
