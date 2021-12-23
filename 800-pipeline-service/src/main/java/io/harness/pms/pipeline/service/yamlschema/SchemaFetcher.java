package io.harness.pms.pipeline.service.yamlschema;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.SchemaCacheKey;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.pipeline.service.yamlschema.exception.YamlSchemaCacheException;
import io.harness.utils.RetryUtils;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;
import io.harness.yaml.schema.client.YamlSchemaClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SchemaFetcher {
  private static final Duration THRESHOLD_PROCESS_DURATION = Duration.ofSeconds(5);
  @Inject @Named("schemaDetailsCache") Cache<SchemaCacheKey, YamlSchemaDetailsWrapper> schemaDetailsCache;
  @Inject @Named("partialSchemaCache") Cache<SchemaCacheKey, PartialSchemaValue> schemaCache;

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
  public PartialSchemaDTO fetchSchema(
      String accountId, ModuleType moduleType, List<YamlSchemaWithDetails> yamlSchemaWithDetailsList) {
    long startTs = System.currentTimeMillis();
    try {
      SchemaCacheKey schemaCacheKey =
          SchemaCacheKey.builder().accountIdentifier(accountId).moduleType(moduleType).build();
      if (schemaCache.containsKey(schemaCacheKey)) {
        return PartialSchemaCacheUtils.getPartialSchemaDTO(schemaCache.get(schemaCacheKey));
      }
      PartialSchemaDTO partialSchemaDTO = fetchSchemaWithRetry(moduleType, accountId, yamlSchemaWithDetailsList);

      schemaCache.put(schemaCacheKey, PartialSchemaCacheUtils.getPartialSchemaValue(partialSchemaDTO));

      log.info("[PMS] Successfully fetched schema for {}", moduleType.name());
      logWarnIfExceedsThreshold(moduleType, startTs);

      return partialSchemaDTO;
    } catch (Exception e) {
      log.warn(format("[PMS] Unable to get %s schema information", moduleType.name()), e);
      return null;
    }
  }

  public YamlSchemaDetailsWrapper fetchSchemaDetail(String accountId, ModuleType moduleType) {
    try {
      SchemaCacheKey schemaCacheKey =
          SchemaCacheKey.builder().accountIdentifier(accountId).moduleType(moduleType).build();
      if (schemaDetailsCache.containsKey(schemaCacheKey)) {
        return schemaDetailsCache.get(schemaCacheKey);
      }
      YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper = fetchSchemaDetailsWithRetry(moduleType, accountId);
      schemaDetailsCache.put(schemaCacheKey, yamlSchemaDetailsWrapper);
      return yamlSchemaDetailsWrapper;
    } catch (Exception e) {
      log.warn(format("[PMS] Unable to get %s schema information", moduleType.name()), e);
      return null;
    }
  }

  public void invalidateAllCache() {
    log.info("[PMS] Invalidating yaml schema cache");
    schemaCache.clear();
    schemaDetailsCache.clear();
    log.info("[PMS] Yaml schema cache was successfully invalidated");
  }

  private YamlSchemaDetailsWrapper fetchSchemaDetailsWithRetry(ModuleType moduleType, String accountIdentifier) {
    try {
      Call<ResponseDTO<YamlSchemaDetailsWrapper>> call =
          obtainYamlSchemaClient(moduleType.name().toLowerCase()).getSchemaDetails(accountIdentifier, null, null, null);

      RetryPolicy<Object> retryPolicy = getRetryPolicy(moduleType);
      return Failsafe.with(retryPolicy).get(() -> SafeHttpCall.execute(call.clone())).getData();
    } catch (Exception e) {
      throw new YamlSchemaCacheException(
          format("[PMS] Unable to get %s schema information", moduleType.name()), e.getCause());
    }
  }

  private PartialSchemaDTO fetchSchemaWithRetry(
      ModuleType moduleType, String accountIdentifier, List<YamlSchemaWithDetails> yamlSchemaWithDetailsList) {
    try {
      Call<ResponseDTO<PartialSchemaDTO>> call =
          obtainYamlSchemaClient(moduleType.name().toLowerCase())
              .get(accountIdentifier, null, null, null,
                  YamlSchemaDetailsWrapper.builder().yamlSchemaWithDetailsList(yamlSchemaWithDetailsList).build());

      RetryPolicy<Object> retryPolicy = getRetryPolicy(moduleType);
      return Failsafe.with(retryPolicy).get(() -> SafeHttpCall.execute(call.clone())).getData();
    } catch (Exception e) {
      log.error(format("[PMS] Unable to get %s schema information", moduleType.name()), e.getCause());
    }
    return null;
  }

  private YamlSchemaClient obtainYamlSchemaClient(String instanceName) {
    return yamlSchemaClientMapper.get(instanceName);
  }

  private RetryPolicy<Object> getRetryPolicy(ModuleType moduleType) {
    return RetryUtils.getRetryPolicy(format("[PMS] [Retrying] Error while calling %s service", moduleType.name()),
        format("[PMS] Error while calling %s service", moduleType.name()), ImmutableList.of(Exception.class),
        Duration.ofMillis(500), 3, log);
  }

  private void logWarnIfExceedsThreshold(ModuleType moduleType, long startTs) {
    Duration processDuration = Duration.ofMillis(System.currentTimeMillis() - startTs);
    if (THRESHOLD_PROCESS_DURATION.compareTo(processDuration) < 0) {
      log.warn("[PMS] Fetching schema for {} service took {}s which is more than threshold of {}s", moduleType.name(),
          processDuration.getSeconds(), THRESHOLD_PROCESS_DURATION.getSeconds());
    }
  }

  // TODO: introduce cache while fetching step schema
  public JsonNode fetchStepYamlSchema(String accountId, EntityType entityType) {
    try {
      Call<ResponseDTO<JsonNode>> call = obtainYamlSchemaClient(entityType.getEntityProduct().name().toLowerCase())
                                             .getStepSchema(accountId, null, null, null, entityType);
      RetryPolicy<Object> retryPolicy = getRetryPolicy(entityType.getEntityProduct());
      return Failsafe.with(retryPolicy).get(() -> SafeHttpCall.execute(call.clone())).getData();

    } catch (Exception e) {
      throw new YamlSchemaCacheException(
          format("[PMS] Unable to get %s step schema information", entityType.getYamlName()), e.getCause());
    }
  }
}
