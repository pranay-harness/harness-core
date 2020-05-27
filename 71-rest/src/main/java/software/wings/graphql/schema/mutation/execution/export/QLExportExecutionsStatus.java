package software.wings.graphql.schema.mutation.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CDC)
public enum QLExportExecutionsStatus implements QLEnum {
  QUEUED,
  READY,
  FAILED,
  EXPIRED;

  @Override
  public String getStringValue() {
    return this.name();
  }

  public static QLExportExecutionsStatus fromStatus(Status status) {
    if (status == null) {
      return null;
    }

    if (status == Status.QUEUED) {
      return QUEUED;
    } else if (status == Status.READY) {
      return READY;
    } else if (status == Status.FAILED) {
      return FAILED;
    } else if (status == Status.EXPIRED) {
      return EXPIRED;
    }

    return null;
  }
}
