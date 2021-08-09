package software.wings.beans.s3;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3Bucket {
  private String name;
  private List<S3File> s3Files;
}
