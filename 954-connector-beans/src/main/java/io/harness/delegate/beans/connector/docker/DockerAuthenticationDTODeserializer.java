package io.harness.delegate.beans.connector.docker;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

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
    }

    return DockerAuthenticationDTO.builder().authType(type).credentials(dockerAuthCredentials).build();
  }

  DockerAuthType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return DockerAuthType.fromString(typeValue);
  }
}
