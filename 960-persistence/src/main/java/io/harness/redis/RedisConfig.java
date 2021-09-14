/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class RedisConfig {
  private boolean sentinel;
  private String masterName;
  private String redisUrl;
  private List<String> sentinelUrls;
  private int connectionMinimumIdleSize;
  private String envNamespace;
  private RedisReadMode readMode;
  private int nettyThreads;
  private boolean useScriptCache;
  private String password;
  private String userName;
  private RedisSSLConfig sslConfig;
}
