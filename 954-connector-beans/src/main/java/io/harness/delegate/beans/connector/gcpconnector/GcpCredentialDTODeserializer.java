/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.gcpconnector;

import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class GcpCredentialDTODeserializer extends StdDeserializer<GcpConnectorCredentialDTO> {
  public GcpCredentialDTODeserializer() {
    super(io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO.class);
  }
  protected GcpCredentialDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public GcpConnectorCredentialDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    GcpCredentialType type = getType(typeNode);
    GcpCredentialSpecDTO gcpCredentialSpecDTO = null;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    if (type == GcpCredentialType.MANUAL_CREDENTIALS) {
      gcpCredentialSpecDTO = mapper.readValue(authSpec.toString(), GcpManualDetailsDTO.class);
    } else if (type == GcpCredentialType.INHERIT_FROM_DELEGATE) {
      if (authSpec != null && !authSpec.isNull()) {
        throw new InvalidRequestException("No spec should be provided with the inherit from delegate type");
      }
    }

    return GcpConnectorCredentialDTO.builder().gcpCredentialType(type).config(gcpCredentialSpecDTO).build();
  }

  GcpCredentialType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return GcpCredentialType.fromString(typeValue);
  }
}
