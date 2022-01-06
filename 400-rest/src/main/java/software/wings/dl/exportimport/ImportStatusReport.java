/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
