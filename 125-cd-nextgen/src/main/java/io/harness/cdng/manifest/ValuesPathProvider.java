package io.harness.cdng.manifest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public interface ValuesPathProvider {
  @JsonIgnore List<String> getValuesPathsToFetch();
}
