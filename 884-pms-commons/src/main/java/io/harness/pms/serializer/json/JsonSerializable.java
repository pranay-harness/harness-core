package io.harness.pms.serializer.json;

public interface JsonSerializable {
  default String toJson() {
    return JsonOrchestrationUtils.asJson(this);
  }
}
