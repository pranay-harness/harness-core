package io.harness.pms.pipeline.service.yamlschema;

import static java.lang.String.format;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.pipeline.service.yamlschema.exception.YamlSchemaCacheException;
import io.harness.utils.RetryUtils;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.client.YamlSchemaClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SchemaFetcher {
  private static final Duration THRESHOLD_PROCESS_DURATION = Duration.ofSeconds(5);

  private static final int CACHE_EVICTION_TIME_HOUR = 1;

  private final LoadingCache<ModuleType, PartialSchemaDTO> schemaCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(CACHE_EVICTION_TIME_HOUR, TimeUnit.HOURS)
          .build(new CacheLoader<ModuleType, PartialSchemaDTO>() {
            @Override
            public PartialSchemaDTO load(@NotNull final ModuleType moduleType) {
              return fetchSchemaWithRetry(moduleType);
            }
          });

  private final Map<String, YamlSchemaClient> yamlSchemaClientMapper;

  @Inject
  public SchemaFetcher(Map<String, YamlSchemaClient> yamlSchemaClientMapper) {
    this.yamlSchemaClientMapper = yamlSchemaClientMapper;
  }

  /**
   * Schema is taken from cache, so every modification will affect cache value.
   * In order to avoid that, we do deep copy of cached object
   */
  @Nullable
  public PartialSchemaDTO fetchSchema(ModuleType moduleType) {
    long startTs = System.currentTimeMillis();
    try {
      PartialSchemaDTO partialSchemaDTO = schemaCache.get(moduleType);

      log.info("[PMS] Successfully fetched schema for {}", moduleType.name());
      logWarnIfExceedsThreshold(moduleType, startTs);

      return PartialSchemaDTO.builder()
          .namespace(partialSchemaDTO.getNamespace())
          .schema(partialSchemaDTO.getSchema().deepCopy())
          .nodeName(partialSchemaDTO.getNodeName())
          .nodeType(partialSchemaDTO.getNodeType())
          .moduleType(partialSchemaDTO.getModuleType())
          .build();
    } catch (Exception e) {
      log.warn(format("[PMS] Unable to get %s schema information", moduleType.name()), e);
      return null;
    }
  }

  public void invalidateCache(ModuleType moduleType) {
    log.info("[PMS] Invalidating yaml schema cache for {}", moduleType.name());
    schemaCache.invalidate(moduleType);
    log.info("[PMS] Yaml schema cache was successfully invalidated for {}", moduleType.name());
  }

  public void invalidateAllCache() {
    log.info("[PMS] Invalidating yaml schema cache");
    schemaCache.invalidateAll();
    log.info("[PMS] Yaml schema cache was successfully invalidated");
  }

  private PartialSchemaDTO fetchSchemaWithRetry(ModuleType moduleType) {
    try {
      Call<ResponseDTO<PartialSchemaDTO>> call =
          obtainYamlSchemaClient(moduleType.name().toLowerCase()).get(null, null, null);

      RetryPolicy<Object> retryPolicy = getRetryPolicy(moduleType);
      return Failsafe.with(retryPolicy).get(() -> SafeHttpCall.execute(call.clone())).getData();
    } catch (Exception e) {
      throw new YamlSchemaCacheException(
          format("[PMS] Unable to get %s schema information", moduleType.name()), e.getCause());
    }
  }

  private YamlSchemaClient obtainYamlSchemaClient(String instanceName) {
    return yamlSchemaClientMapper.get(instanceName);
  }

  private RetryPolicy<Object> getRetryPolicy(ModuleType moduleType) {
    return RetryUtils.getRetryPolicy(format("[PMS] [Retrying] Error while calling %s service", moduleType.name()),
        format("[PMS] Error while calling %s service", moduleType.name()), ImmutableList.of(Exception.class),
        Duration.ofSeconds(1), 3, log);
  }

  private void logWarnIfExceedsThreshold(ModuleType moduleType, long startTs) {
    Duration processDuration = Duration.ofMillis(System.currentTimeMillis() - startTs);
    if (THRESHOLD_PROCESS_DURATION.compareTo(processDuration) < 0) {
      log.warn("[PMS] Fetching schema for {} service took {}s which is more than threshold of {}s", moduleType.name(),
          processDuration.getSeconds(), THRESHOLD_PROCESS_DURATION.getSeconds());
    }
  }
}
