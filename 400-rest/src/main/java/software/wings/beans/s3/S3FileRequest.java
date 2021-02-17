package software.wings.beans.s3;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class S3FileRequest {
  private String bucketName;
  private List<String> fileKeys;
}
