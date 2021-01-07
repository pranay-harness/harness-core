package io.harness.pms.exception;

import io.harness.pms.serializer.json.JsonSerializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YamlNodeErrorInfo implements JsonSerializable {
  String identifier;
  String name;
  String type;
}
