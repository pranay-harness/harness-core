/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.dl.exportimport;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * This value class is used by the import API to report the import status.
 *
 * @author marklu on 11/15/18
 */
@Data
@Builder
public class ImportStatusReport {
  private ImportMode mode;
  private List<ImportStatus> statuses;

  @Data
  @Builder
  public static class ImportStatus {
    private String collectionName;
    private int imported;
    private int idClashes;
  }
}
