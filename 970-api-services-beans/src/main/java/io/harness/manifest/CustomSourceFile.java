package io.harness.manifest;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomSourceFile {
  String filePath;
  String fileContent;
}
