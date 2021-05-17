package io.harness.cvng.beans.appd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tier implements Comparable<Tier> {
  long id;
  String name;

  @Override
  public int compareTo(@NotNull Tier o) {
    return name.compareTo(o.name);
  }
}
