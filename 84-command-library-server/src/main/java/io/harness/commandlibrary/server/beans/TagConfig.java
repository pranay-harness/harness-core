package io.harness.commandlibrary.server.beans;

import io.harness.commandlibrary.server.utils.JsonSerializable;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class TagConfig implements JsonSerializable {
  Set<String> allowedTags;
  Set<String> importantTags;
}
