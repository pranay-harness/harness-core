/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(PL)
public class AwsKmsCredentialDTODeserializer extends StdDeserializer<AwsKmsConnectorCredentialDTO> {
  public AwsKmsCredentialDTODeserializer() {
    super(AwsKmsConnectorCredentialDTO.class);
  }

  protected AwsKmsCredentialDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public AwsKmsConnectorCredentialDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    AwsKmsCredentialType type = getType(typeNode);
    AwsKmsCredentialSpecDTO awsKmsCredentialSpecDTO = null;
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();

    switch (type) {
      case MANUAL_CONFIG:
        awsKmsCredentialSpecDTO = mapper.readValue(authSpec.toString(), AwsKmsCredentialSpecManualConfigDTO.class);
        break;
      case ASSUME_IAM_ROLE:
        awsKmsCredentialSpecDTO = mapper.readValue(authSpec.toString(), AwsKmsCredentialSpecAssumeIAMDTO.class);
        break;
      case ASSUME_STS_ROLE:
        awsKmsCredentialSpecDTO = mapper.readValue(authSpec.toString(), AwsKmsCredentialSpecAssumeSTSDTO.class);
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return AwsKmsConnectorCredentialDTO.builder().credentialType(type).config(awsKmsCredentialSpecDTO).build();
  }

  AwsKmsCredentialType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return AwsKmsCredentialType.fromString(typeValue);
  }
}
