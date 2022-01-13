/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
