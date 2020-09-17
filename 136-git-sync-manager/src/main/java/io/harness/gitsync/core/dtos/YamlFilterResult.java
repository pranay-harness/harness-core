package io.harness.gitsync.core.dtos;

import io.harness.git.model.GitFileChange;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class YamlFilterResult {
  @Singular List<GitFileChange> filteredFiles;
  @Singular("excludedFilePathWithReason") Map<String, String> excludedFilePathWithReasonMap;
}
