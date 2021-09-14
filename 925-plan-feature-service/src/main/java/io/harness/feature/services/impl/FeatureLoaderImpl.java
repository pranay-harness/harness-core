package io.harness.feature.services.impl;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.RateLimitRestriction;
import io.harness.feature.bases.Restriction;
import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.configs.ClientInfo;
import io.harness.feature.configs.FeaturesConfig;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.services.FeatureLoader;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class FeatureLoaderImpl implements FeatureLoader {
  private final FeatureServiceImpl featureService;
  private final List<FeaturesConfig> featuresConfigs;
  private final KryoConverterFactory kryoConverterFactory;
  private Map<String, FeatureUsageClient> clientMap = new HashMap<>();

  private static final String FEATURE_RESOURCE_PATH = "classpath:features/*.yml";

  @Inject
  public FeatureLoaderImpl(FeatureServiceImpl featureService, KryoConverterFactory kryoConverterFactory) {
    featuresConfigs = new ArrayList<>();
    this.featureService = featureService;
    this.kryoConverterFactory = kryoConverterFactory;
  }

  public void run(Object applicationConfiguration) {
    loadYmlToFeaturesConfig(featuresConfigs);

    for (FeaturesConfig featuresConfig : featuresConfigs) {
      initiateClientMap(featuresConfig, applicationConfiguration);

      for (Feature feature : featuresConfig.getFeatures()) {
        try {
          validAndCompleteFeature(feature, featuresConfig.getModuleType());
          featureService.registerFeature(feature.getName(), feature);
        } catch (Exception e) {
          log.error(
              String.format("Got exception during generate feature [%s], uncover and continue", feature.getName()), e);
        }
      }
    }
  }

  private FeatureUsageClientFactory generateUsageClientFactory(ClientInfo clientInfo, Object applicationConfiguration) {
    ServiceHttpClientConfig serviceHttpClientConfig;
    String secret;
    try {
      String[] serverFields = clientInfo.getClientConfig().split("\\.");
      serviceHttpClientConfig = lookForField(serverFields, applicationConfiguration, ServiceHttpClientConfig.class);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      log.error(String.format("Failed when looking for client config [%s]", clientInfo.getClientConfig()), e);
      return null;
    }

    try {
      String[] secretFields = clientInfo.getSecretConfig().split("\\.");
      secret = lookForField(secretFields, applicationConfiguration, String.class);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      log.error(String.format("Failed when looking for secret config [%s]", clientInfo.getSecretConfig()), e);
      return null;
    }

    if (secret == null || serviceHttpClientConfig == null) {
      return null;
    }
    return new FeatureUsageClientFactory(
        serviceHttpClientConfig, secret, new ServiceTokenGenerator(), kryoConverterFactory, NG_MANAGER.getServiceId());
  }

  private <T> T lookForField(String[] fields, Object applicationConfiguration, Class<T> clazz)
      throws IllegalAccessException, NoSuchFieldException {
    Field targetField = applicationConfiguration.getClass().getDeclaredField(fields[0]);
    targetField.setAccessible(true);
    Object targetObject = targetField.get(applicationConfiguration);
    targetField.setAccessible(false);
    for (int i = 1; i < fields.length; i++) {
      if (targetObject == null) {
        return null;
      }
      targetField = targetObject.getClass().getDeclaredField(fields[i]);
      targetField.setAccessible(true);
      targetObject = targetField.get(targetObject);
      targetField.setAccessible(false);
    }
    if (clazz.isInstance(targetObject)) {
      return (T) targetObject;
    }
    return null;
  }

  private void validAndCompleteFeature(Feature feature, ModuleType moduleType) {
    validFeatureInfo(feature);
    feature.setModuleType(moduleType);

    feature.getRestrictions().values().forEach(v -> {
      try {
        validRestriction(v);
        loadUsageClientToLimitRestriction(v);
      } catch (Exception e) {
        throw new InvalidArgumentsException(
            String.format("Failed to generate restriction for feature [%s]", feature.getName()), e,
            WingsException.USER_SRE);
      }
    });
  }

  private void loadUsageClientToLimitRestriction(Restriction restriction) {
    if (RestrictionType.STATIC_LIMIT.equals(restriction.getRestrictionType())
        || RestrictionType.RATE_LIMIT.equals(restriction.getRestrictionType())) {
      LimitRestriction limitRestriction = (LimitRestriction) restriction;
      limitRestriction.setFeatureUsageClient(findFeatureClient(limitRestriction.getClientName()));
    }
  }

  private FeatureUsageClient findFeatureClient(String clientName) {
    FeatureUsageClient featureUsageClient = clientMap.get(clientName);
    if (featureUsageClient == null) {
      throw new InvalidArgumentsException(String.format("Client [%s] is not defined", clientName));
    }
    return featureUsageClient;
  }

  private void validFeatureInfo(Feature feature) {
    if (feature.getName() == null) {
      throw new InvalidArgumentsException("Feature is missing name");
    }

    if (feature.getRestrictions() == null) {
      throw new InvalidArgumentsException(
          String.format("Missing mandatory information for feature [%s]", feature.getName()));
    }
  }

  private void validRestriction(Restriction restriction) {
    if (restriction.getRestrictionType() == null) {
      throw new InvalidArgumentsException("Restriction type is missing");
    }

    switch (restriction.getRestrictionType()) {
      case AVAILABILITY:
        EnableDisableRestriction enableDisableRestriction = (EnableDisableRestriction) restriction;

        if (enableDisableRestriction.getEnabled() == null) {
          throw new InvalidArgumentsException("EnableDisableRestriction is missing enabled value");
        }
        break;
      case RATE_LIMIT:
        RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;

        if (rateLimitRestriction.getClientName() == null || rateLimitRestriction.getLimit() == null
            || rateLimitRestriction.getTimeUnit() == null) {
          throw new InvalidArgumentsException("RateLimitRestriction is missing necessary config");
        }
        break;
      case STATIC_LIMIT:
        StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;

        if (staticLimitRestriction.getClientName() == null || staticLimitRestriction.getLimit() == null) {
          throw new InvalidArgumentsException("StaticLimitRestriction is missing necessary config");
        }
        break;
      default:
        throw new InvalidArgumentsException("Unknown restriction type");
    }
  }

  private void loadYmlToFeaturesConfig(List<FeaturesConfig> configs) {
    ClassLoader cl = this.getClass().getClassLoader();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    try {
      Resource[] resources = resolver.getResources(FEATURE_RESOURCE_PATH);

      ObjectMapper om = new ObjectMapper(new YAMLFactory());
      for (Resource resource : resources) {
        byte[] bytes = Resources.toByteArray(resource.getURL());
        FeaturesConfig featuresConfig = om.readValue(bytes, FeaturesConfig.class);
        configs.add(featuresConfig);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load feature yaml file.", e);
    }
  }

  private void initiateClientMap(FeaturesConfig featuresConfig, Object applicationConfiguration) {
    for (ClientInfo clientInfo : featuresConfig.getClients()) {
      FeatureUsageClientFactory featureUsageClientFactory =
          generateUsageClientFactory(clientInfo, applicationConfiguration);
      if (featureUsageClientFactory != null) {
        clientMap.put(clientInfo.getName(), featureUsageClientFactory.get());
      }
    }
  }
}
