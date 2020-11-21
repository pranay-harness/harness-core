package software.wings.security.authentication;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class BatchQueryConfig {
  private int queryBatchSize;
  private int instanceDataBatchSize;
  private boolean syncJobDisabled;
}
