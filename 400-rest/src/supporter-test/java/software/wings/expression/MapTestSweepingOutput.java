package software.wings.expression;

import io.harness.beans.SweepingOutput;

import java.util.HashMap;

public class MapTestSweepingOutput extends HashMap<String, Object> implements SweepingOutput {
  @Override
  public String getType() {
    return "mapTestSweepingOutput";
  }
}
