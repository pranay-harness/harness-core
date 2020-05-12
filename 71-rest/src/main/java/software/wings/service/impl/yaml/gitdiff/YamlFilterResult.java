package software.wings.service.impl.yaml.gitdiff;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.beans.yaml.GitFileChange;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class YamlFilterResult {
  @Singular List<GitFileChange> filteredFiles;
  @Singular("excludedFilePathWithReason") Map<String, String> excludedFilePathWithReasonMap;
}
