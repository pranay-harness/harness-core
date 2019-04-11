package io.harness.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileData {
  private String filePath;
  private String fileContent;
}
