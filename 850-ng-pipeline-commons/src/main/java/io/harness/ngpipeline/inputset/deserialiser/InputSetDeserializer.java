package io.harness.ngpipeline.inputset.deserialiser;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@ToBeDeleted
@Deprecated
public class InputSetDeserializer extends StdDeserializer<InputSetConfig> {
  public InputSetDeserializer() {
    super(InputSetDeserializer.class);
  }

  public InputSetDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public InputSetConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode parentJsonNode = jp.getCodec().readTree(jp);
    JsonNode pipelineJsonNode = parentJsonNode.get("pipeline");

    ObjectNode pipelineNode = ((ObjectNode) parentJsonNode).putObject("pipeline");
    pipelineNode.set("pipeline", pipelineJsonNode);

    NgPipeline ngPipeline = YamlPipelineUtils.read(YamlPipelineUtils.writeString(pipelineNode), NgPipeline.class);
    String identifier = getValueFromJsonNode(parentJsonNode, "identifier");
    String name = getValueFromJsonNode(parentJsonNode, "name");
    String description = getValueFromJsonNode(parentJsonNode, "description");

    JsonNode tagsJsonNode = parentJsonNode.get("tags");
    Map<String, String> tags = null;
    if (tagsJsonNode != null) {
      tags = YamlPipelineUtils.read(YamlPipelineUtils.writeString(tagsJsonNode), LinkedHashMap.class);
    }
    return InputSetConfig.builder()
        .identifier(identifier)
        .description(description)
        .pipeline(ngPipeline)
        .name(name)
        .tags(tags)
        .build();
  }

  private String getValueFromJsonNode(JsonNode parentJsonNode, String fieldName) {
    JsonNode fieldNode = parentJsonNode.get(fieldName);
    String result = "";
    if (fieldNode != null) {
      result = fieldNode.asText();
    }
    return result;
  }
}
