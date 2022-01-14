package io.harness.remote;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class CEGcpSetupConfig {
  private String gcpProjectId;
}