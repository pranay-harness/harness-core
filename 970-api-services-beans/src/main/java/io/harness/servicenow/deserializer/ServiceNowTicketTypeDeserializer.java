package io.harness.servicenow.deserializer;

import io.harness.servicenow.ServiceNowTicketTypeDTO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class ServiceNowTicketTypeDeserializer extends StdDeserializer<ServiceNowTicketTypeDTO> {
  public ServiceNowTicketTypeDeserializer() {
    this(null);
  }

  public ServiceNowTicketTypeDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ServiceNowTicketTypeDTO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new ServiceNowTicketTypeDTO(node);
  }
}
