package software.wings.beans.s3;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FetchS3FilesExecutionResponse implements DelegateTaskNotifyResponseData {
  private S3FetchFileResult s3FetchFileResult;
  private FetchS3FilesCommandStatus commandStatus;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;

  public enum FetchS3FilesCommandStatus { SUCCESS, FAILURE }
}
