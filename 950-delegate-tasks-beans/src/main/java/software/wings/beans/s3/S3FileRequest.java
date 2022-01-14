package software.wings.beans.s3;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class S3FileRequest {
  private String bucketName;
  private List<String> fileKeys;
}
