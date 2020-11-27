package io.harness.pms.beans;

import io.harness.serializer.JsonUtils;

public interface JsonSerializable {
  default String toJson() {
    return JsonUtils.asJson(this);
  }
}
