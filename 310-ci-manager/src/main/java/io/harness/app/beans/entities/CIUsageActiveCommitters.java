package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIUsageActiveCommitters {
  private int count;
  private String displayName;
  private List<CIUsageReference> references;
}
