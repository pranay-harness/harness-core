package io.harness.servicenow.deserializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.servicenow.ServiceNowTicketNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class ServiceNowTicketDeserializer extends StdDeserializer<ServiceNowTicketNG> {
  public ServiceNowTicketDeserializer() {
    this(null);
  }

  public ServiceNowTicketDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ServiceNowTicketNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new ServiceNowTicketNG(node);
  }
}
