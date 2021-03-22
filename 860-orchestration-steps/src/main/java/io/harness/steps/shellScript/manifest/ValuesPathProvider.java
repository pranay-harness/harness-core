package io.harness.steps.shellScript.manifest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public interface ValuesPathProvider {
  @JsonIgnore List<String> getValuesPathsToFetch();
}
