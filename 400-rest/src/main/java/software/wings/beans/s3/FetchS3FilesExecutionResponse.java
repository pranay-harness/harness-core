package software.wings.beans.s3;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class FetchS3FilesExecutionResponse implements DelegateTaskNotifyResponseData {
  private S3FetchFileResult s3FetchFileResult;
  private FetchS3FilesCommandStatus commandStatus;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;

  public enum FetchS3FilesCommandStatus { SUCCESS, FAILURE }
}
