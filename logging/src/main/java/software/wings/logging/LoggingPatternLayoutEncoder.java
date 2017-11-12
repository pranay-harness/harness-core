package software.wings.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

public class LoggingPatternLayoutEncoder extends PatternLayoutEncoder {
  @Override
  public void start() {
    // put your converter
    PatternLayout.defaultConverterMap.put("process_id", ProcessIdConverter.class.getName());
    super.start();
  }
}
