/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
