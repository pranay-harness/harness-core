package software.wings.beans.s3;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class S3FetchFileResult {
  private List<S3Bucket> s3Buckets;
}
