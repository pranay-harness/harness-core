package io.harness.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileData {
  String filePath;
  byte[] fileBytes;
  String fileName;
  String fileContent;
}
