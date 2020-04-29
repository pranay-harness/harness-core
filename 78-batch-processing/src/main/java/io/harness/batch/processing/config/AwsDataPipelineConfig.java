package io.harness.batch.processing.config;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class AwsDataPipelineConfig {
  private String gcpServiceAccount;
  private String gcpProjectId;
  private String gcsBasePath;
}
