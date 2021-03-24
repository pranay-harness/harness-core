package io.harness.jira.deserializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraIssueCreateMetaResponseNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class JiraCreateMetaResponseDeserializer extends StdDeserializer<JiraIssueCreateMetaResponseNG> {
  public JiraCreateMetaResponseDeserializer() {
    this(null);
  }

  public JiraCreateMetaResponseDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JiraIssueCreateMetaResponseNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new JiraIssueCreateMetaResponseNG(node);
  }
}
