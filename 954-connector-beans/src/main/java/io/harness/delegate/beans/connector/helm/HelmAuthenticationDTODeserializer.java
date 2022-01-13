/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.helm;

import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class HelmAuthenticationDTODeserializer extends StdDeserializer<HttpHelmAuthenticationDTO> {
  public HelmAuthenticationDTODeserializer() {
    super(HelmAuthenticationDTODeserializer.class);
  }

  public HelmAuthenticationDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public HttpHelmAuthenticationDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    HttpHelmAuthType type = getType(typeNode);
    HttpHelmAuthCredentialsDTO helmAuthCredentials = null;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    if (type == HttpHelmAuthType.USER_PASSWORD) {
      helmAuthCredentials = mapper.readValue(authSpec.toString(), HttpHelmUsernamePasswordDTO.class);
    } else if (type == HttpHelmAuthType.ANONYMOUS) {
      if (authSpec != null && !authSpec.isNull()) {
        throw new InvalidRequestException("No spec should be provided with the anonymous type");
      }
    }

    return HttpHelmAuthenticationDTO.builder().authType(type).credentials(helmAuthCredentials).build();
  }

  HttpHelmAuthType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return HttpHelmAuthType.fromString(typeValue);
  }
}
