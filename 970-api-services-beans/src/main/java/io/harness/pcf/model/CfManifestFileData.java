package io.harness.pcf.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.File;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class CfManifestFileData {
  private File manifestFile;
  private List<File> varFiles;
}
