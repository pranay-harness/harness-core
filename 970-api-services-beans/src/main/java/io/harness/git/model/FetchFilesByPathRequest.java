package io.harness.git.model;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class FetchFilesByPathRequest extends GitBaseRequest {
  private List<String> filePaths;
  private List<String> fileExtensions;
  private boolean recursive;
}
