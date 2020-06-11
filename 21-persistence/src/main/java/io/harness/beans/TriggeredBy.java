package io.harness.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggeredBy {
  private String name;
  private String email;

  public static TriggeredBy triggeredBy(String name, String email) {
    return new TriggeredBy(name, email);
  }
}
