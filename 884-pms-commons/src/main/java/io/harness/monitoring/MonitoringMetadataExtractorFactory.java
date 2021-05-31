package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class MonitoringMetadataExtractorFactory {
  @Inject Injector injector;
  private static Map<Class<?>, MonitoringMetadataExtractor<?>> eventTypeToMonitoringMetadaExtractor = new HashMap<>();

  public <T extends Message> MonitoringMetadataExtractor<T> getMetadataExtractor(Class<?> clazz) {
    if (eventTypeToMonitoringMetadaExtractor.isEmpty()) {
      populateEventTypeToMonitoringMetadataExtractorMap();
    }
    return (MonitoringMetadataExtractor<T>) eventTypeToMonitoringMetadaExtractor.get(clazz);
  }

  private void populateEventTypeToMonitoringMetadataExtractorMap() {
    Reflections reflections = new Reflections("io.harness");
    Set<Class<? extends MonitoringMetadataExtractor>> classes =
        reflections.getSubTypesOf(MonitoringMetadataExtractor.class);

    try {
      classes.forEach(subClass -> {
        try {
          MonitoringMetadataExtractor<?> extractor = injector.getInstance(subClass);
          eventTypeToMonitoringMetadaExtractor.put(extractor.getType(), extractor);
        } catch (Exception e) {
          log.error("Exception while getting a monitoring metadata extractor", e);
        }
      });
    } catch (Exception ex) {
      log.error("Exception while getting a monitoring metadata extractor", ex);
    }
  }
}
