package software.wings.beans.s3;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class S3FileRequest {
  private String bucketName;
  private List<String> fileKeys;
}
