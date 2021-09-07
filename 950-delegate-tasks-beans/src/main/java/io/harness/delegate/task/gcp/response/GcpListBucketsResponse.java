package io.harness.delegate.task.gcp.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.dto.ErrorDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class GcpListBucketsResponse implements GcpTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private ErrorDetail errorDetail;
  private CommandExecutionStatus commandExecutionStatus;
  private List<GcpBucketDetails> buckets;
}
