package software.wings.beans.s3;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3FetchFileResult {
  private List<S3Bucket> s3Buckets;
}
