package software.wings.service.impl.instana;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanaTimeFrame {
  long to;
  long windowSize;
}