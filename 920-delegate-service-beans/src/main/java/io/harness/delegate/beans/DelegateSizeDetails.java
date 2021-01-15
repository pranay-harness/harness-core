package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateSizeDetails {
  private DelegateSize size;
  private int taskLimit;
  private int replicas;

  /**
   * Amount of RAM in MBs
   */
  private int ram;

  /**
   * Number of CPUs
   */
  private double cpu;

  /**
   * Amount of disk space in GBs
   */
  private int disk;
}
