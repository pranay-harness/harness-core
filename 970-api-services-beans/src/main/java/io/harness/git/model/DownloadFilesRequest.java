package io.harness.git.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class DownloadFilesRequest extends FetchFilesByPathRequest {
  private String destinationDirectory;
}
