package io.harness.delegate.task.pcf;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

@Data
@Builder
public class PcfManifestFileData {
  private File manifestFile;
  private List<File> varFiles;
}
