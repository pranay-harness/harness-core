package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class PodInfoConfig {
  private String name;
  private int replica;
  private int isolatedReplica;
}
