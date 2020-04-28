package io.harness.commandlibrary.server.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandManifest {
  String name;
  String version;
  String displayName;
  String description;
  String category;
  String type;
}
