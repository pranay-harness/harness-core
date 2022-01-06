/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awssecretmanager;

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
public class AwsSMCredentialDTODeserializer extends StdDeserializer<AwsSecretManagerCredentialDTO> {
  public AwsSMCredentialDTODeserializer() {
    super(AwsSecretManagerCredentialDTO.class);
  }

  protected AwsSMCredentialDTODeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public AwsSecretManagerCredentialDTO deserialize(JsonParser jp, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode authSpec = parentJsonNode.get("spec");

    AwsSecretManagerCredentialType type = getType(typeNode);
    AwsSecretManagerCredentialSpecDTO awsSecretManagerCredentialSpecDTO = null;
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();

    switch (type) {
      case MANUAL_CONFIG:
        awsSecretManagerCredentialSpecDTO =
            mapper.readValue(authSpec.toString(), AwsSMCredentialSpecManualConfigDTO.class);
        break;
      case ASSUME_IAM_ROLE:
        if (authSpec != null && !authSpec.isNull()) {
          throw new InvalidRequestException("No spec should be provided with the credential type assume IAM role");
        }
        break;
      case ASSUME_STS_ROLE:
        awsSecretManagerCredentialSpecDTO =
            mapper.readValue(authSpec.toString(), AwsSMCredentialSpecAssumeSTSDTO.class);
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }

    return AwsSecretManagerCredentialDTO.builder()
        .credentialType(type)
        .config(awsSecretManagerCredentialSpecDTO)
        .build();
  }

  AwsSecretManagerCredentialType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return AwsSecretManagerCredentialType.fromString(typeValue);
  }
}
