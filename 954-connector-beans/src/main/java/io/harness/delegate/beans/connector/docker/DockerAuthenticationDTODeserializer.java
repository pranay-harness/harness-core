/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class DockerAuthenticationDTODeserializer extends StdDeserializer<DockerAuthenticationDTO> {
  public DockerAuthenticationDTODeserializer() {
    super(DockerAuthenticationDTODeserializer.class);
  }

  public DockerAuthenticationDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public DockerAuthenticationDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    DockerAuthType type = getType(typeNode);
    DockerAuthCredentialsDTO dockerAuthCredentials = null;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    if (type == DockerAuthType.USER_PASSWORD) {
      dockerAuthCredentials = mapper.readValue(authSpec.toString(), DockerUserNamePasswordDTO.class);
    } else if (type == DockerAuthType.ANONYMOUS) {
      if (authSpec != null && !authSpec.isNull()) {
        throw new InvalidRequestException("No spec should be provided with the anonymous type");
      }
    }

    return DockerAuthenticationDTO.builder().authType(type).credentials(dockerAuthCredentials).build();
  }

  DockerAuthType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return DockerAuthType.fromString(typeValue);
  }
}
