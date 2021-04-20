package io.harness.jira.deserializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraStatusNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class JiraStatusDeserializer extends StdDeserializer<JiraStatusNG> {
  public JiraStatusDeserializer() {
    this(null);
  }

  public JiraStatusDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JiraStatusNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new JiraStatusNG(node);
  }
}
