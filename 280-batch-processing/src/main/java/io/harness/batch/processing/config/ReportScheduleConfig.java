package io.harness.batch.processing.config;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class ReportScheduleConfig {
  private boolean enabled;
  private String templateName;
}
