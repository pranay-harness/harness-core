package io.harness.delegate.task.pcf;

import java.io.File;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfManifestFileData {
  private File manifestFile;
  private List<File> varFiles;
}
