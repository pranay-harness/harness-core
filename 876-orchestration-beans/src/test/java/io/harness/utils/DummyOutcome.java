package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@AllArgsConstructor
@TypeAlias("dummyOutcome25")
@JsonTypeName("dummyOutcome25")
public class DummyOutcome implements Outcome {
  String name;

  @Override
  public String getType() {
    return "dummyOutcome25";
  }
}
