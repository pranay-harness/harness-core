package software.wings.beans.s3;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3FileRequest {
  private String bucketName;
  private List<String> fileKeys;
}
