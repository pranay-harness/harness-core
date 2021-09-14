/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.deserializer;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import io.harness.ng.core.EntityDetail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class EntityDetailDeserializer extends StdDeserializer<EntityDetail> {
  public EntityDetailDeserializer() {
    super(EntityDetailDeserializer.class);
  }

  public EntityDetailDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public EntityDetail deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode typeNode = parentJsonNode.get("type");
    JsonNode entityRefNode = parentJsonNode.get("entityRef");
    JsonNode nameNode = parentJsonNode.get("name");

    EntityType type = getType(typeNode);
    EntityReference reference;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    if (type == EntityType.INPUT_SETS) {
      reference = mapper.readValue(entityRefNode.toString(), InputSetReference.class);
    } else {
      reference = mapper.readValue(entityRefNode.toString(), IdentifierRef.class);
    }

    String name = nameNode != null ? nameNode.asText() : null;

    return EntityDetail.builder().type(type).entityRef(reference).name(name).build();
  }

  EntityType getType(JsonNode typeNode) {
    String typeValue = typeNode.asText();
    return EntityType.fromString(typeValue);
  }
}
