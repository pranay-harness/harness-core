package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class ArtifactoryAuthDTODeserializer extends StdDeserializer<ArtifactoryAuthenticationDTO> {
  public ArtifactoryAuthDTODeserializer() {
    super(io.harness.delegate.beans.connector.nexusconnector.NexusAuthDTODeserializer.class);
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
    }

    return ArtifactoryAuthenticationDTO.builder().authType(type).credentials(artifactoryAuthCredentialsDTO).build();
  }

  ArtifactoryAuthType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return ArtifactoryAuthType.fromString(typeValue);
  }
}
