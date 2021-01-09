package io.harness.plancreator.beans;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("variablesOutcome")
@JsonTypeName("variablesOutcome")
public class VariablesOutcome extends HashMap<String, Object> implements Outcome {
  @Override
  public String getType() {
    return "variablesOutcome";
  }
}
