package io.harness.servicenow;

import io.harness.jira.JiraFieldAllowedValueNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class ServiceNowFieldAllowedValueDeserializer extends StdDeserializer<JiraFieldAllowedValueNG> {
  public ServiceNowFieldAllowedValueDeserializer() {
    this(null);
  }

  public ServiceNowFieldAllowedValueDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JiraFieldAllowedValueNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new JiraFieldAllowedValueNG(node);
  }
}
