package io.harness.pcf.model;

import java.io.File;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CfManifestFileData {
  private File manifestFile;
  private List<File> varFiles;
}
