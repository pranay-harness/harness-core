package software.wings.beans.s3;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class S3Bucket {
  private String name;
  private List<S3File> s3Files;
}
