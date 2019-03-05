package software.wings.dl.exportimport;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
