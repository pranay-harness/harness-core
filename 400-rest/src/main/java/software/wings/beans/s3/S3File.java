package software.wings.beans.s3;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3File {
  private String fileKey;
  private String fileContent;
}
