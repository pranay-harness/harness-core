/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.artifactoryconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthDTODeserializer;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class ArtifactoryAuthDTODeserializer extends StdDeserializer<ArtifactoryAuthenticationDTO> {
  public ArtifactoryAuthDTODeserializer() {
    super(NexusAuthDTODeserializer.class);
  }

  public ArtifactoryAuthDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ArtifactoryAuthenticationDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    ArtifactoryAuthType type = getType(typeNode);
    ArtifactoryAuthCredentialsDTO artifactoryAuthCredentialsDTO = null;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    if (type == ArtifactoryAuthType.USER_PASSWORD) {
      artifactoryAuthCredentialsDTO = mapper.readValue(authSpec.toString(), ArtifactoryUsernamePasswordAuthDTO.class);
    } else if (type == ArtifactoryAuthType.ANONYMOUS) {
      if (authSpec != null && !authSpec.isNull()) {
        throw new InvalidRequestException("No spec should be provided with the anonymous type");
      }
    }

    return ArtifactoryAuthenticationDTO.builder().authType(type).credentials(artifactoryAuthCredentialsDTO).build();
  }

  ArtifactoryAuthType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return ArtifactoryAuthType.fromString(typeValue);
  }
}
