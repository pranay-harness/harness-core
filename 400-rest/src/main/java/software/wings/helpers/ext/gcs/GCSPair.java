package software.wings.helpers.ext.gcs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GCSPair {
  String objectName;
  String updatedTime;
}
