package io.harness.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlAdminClientConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.MongoConfig;
import io.harness.redis.RedisConfig;
import io.harness.remote.client.ServiceHttpClientConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class ResourceGroupServiceConfig {
  @JsonProperty("enableResourceGroup") boolean enableResourceGroup;
  @JsonProperty("mongo") MongoConfig mongoConfig;
  @JsonProperty("resourceClients") ResourceClientConfigs resourceClientConfigs;
  @JsonProperty("redis") RedisConfig redisConfig;
  @JsonProperty("accessControlAdminClient") AccessControlAdminClientConfiguration accessControlAdminClientConfiguration;
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty(value = "enableAudit") private boolean enableAudit;
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
}
