package io.harness.serializer.jackson;

import io.harness.serializer.jackson.json.RecastDocumentSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.Document;

public class PipelineServiceJacksonModule extends SimpleModule {
  public PipelineServiceJacksonModule() {
    addSerializer(Document.class, new RecastDocumentSerializer());
  }
}
