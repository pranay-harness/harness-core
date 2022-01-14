package io.harness.cvng.beans.activity.cdng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("CDNG")
@OwnedBy(HarnessTeam.CV)
public class CDNGActivitySourceDTO extends ActivitySourceDTO {
  @Override
  public ActivitySourceType getType() {
    return ActivitySourceType.CDNG;
  }

  @Override
  public boolean isEditable() {
    return false;
  }
}
