package io.harness.feature.services.impl;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.feature.EnforcementConfiguration;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.RateLimitRestriction;
import io.harness.feature.bases.Restriction;
import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.configs.FeatureInfo;
import io.harness.feature.configs.FeatureName;
import io.harness.feature.configs.FeaturesConfig;
import io.harness.feature.configs.RestrictionConfig;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.RateLimitInterface;
import io.harness.feature.interfaces.StaticLimitInterface;
import io.harness.feature.services.FeatureLoader;
import io.harness.licensing.Edition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class FeatureLoaderImpl implements FeatureLoader {
  private final FeatureServiceImpl featureService;
  private final EnforcementConfiguration enforcementConfiguration;
  private final List<FeaturesConfig> featuresConfigs;

  @Inject
  public FeatureLoaderImpl(FeatureServiceImpl featureService, EnforcementConfiguration enforcementConfiguration) {
    featuresConfigs = new ArrayList<>();
    this.featureService = featureService;
    this.enforcementConfiguration = enforcementConfiguration;
  }

  public void run(Injector injector) {
    for (String ymlPath : enforcementConfiguration.getFeatureYmlPaths()) {
      loadYmlToFeaturesConfig(ymlPath, featuresConfigs);
    }

    for (FeaturesConfig featuresConfig : featuresConfigs) {
      for (FeatureInfo featureInfo : featuresConfig.getFeatures()) {
        Feature feature = generateFeature(featureInfo, featuresConfig.getModuleType(), injector);
        featureService.registerFeature(feature.getName(), feature);
      }
    }
  }

  private Feature generateFeature(FeatureInfo featureInfo, ModuleType moduleType, Injector injector) {
    validFeatureInfo(featureInfo);
    Map<Edition, Restriction> restrictionMap =
        featureInfo.getRestrictions().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> {
          try {
            return generateRestriction(v.getValue(), injector);
          } catch (Exception e) {
            throw new InvalidArgumentsException(String.format("Failed to generate [%s] restriction for feature [%s]",
                                                    v.getKey(), featureInfo.getName()),
                e, WingsException.USER_SRE);
          }
        }));
    return new Feature(FeatureName.valueOf(featureInfo.getName().trim()), featureInfo.getDescription().trim(),
        moduleType, restrictionMap);
  }

  private Restriction generateRestriction(RestrictionConfig restrictionConfig, Injector injector)
      throws ClassNotFoundException {
    RestrictionType restrictionType = restrictionConfig.getRestrictionType();
    switch (restrictionType) {
      case AVAILABILITY:
        if (restrictionConfig.getEnabled() == null) {
          throw new InvalidArgumentsException("Enabled is null");
        }
        return new EnableDisableRestriction(restrictionConfig.getRestrictionType(), restrictionConfig.getEnabled());
      case RATE_LIMIT:
        if (restrictionConfig.getLimit() == null || restrictionConfig.getTimeUnit() == null
            || restrictionConfig.getImplClass() == null) {
          throw new InvalidArgumentsException("Invalid RateLimitRestriction definition");
        }
        RateLimitInterface rateLimitUsageImpl =
            findImplementationInstance(restrictionConfig.getImplClass(), RateLimitInterface.class, injector);

        return new RateLimitRestriction(restrictionConfig.getRestrictionType(), restrictionConfig.getLimit(),
            restrictionConfig.getTimeUnit(), rateLimitUsageImpl);
      case STATIC_LIMIT:
        if (restrictionConfig.getLimit() == null || restrictionConfig.getImplClass() == null) {
          throw new InvalidArgumentsException("Invalid RateLimitRestriction definition");
        }
        StaticLimitInterface staticLimit =
            findImplementationInstance(restrictionConfig.getImplClass(), StaticLimitInterface.class, injector);

        return new StaticLimitRestriction(
            restrictionConfig.getRestrictionType(), restrictionConfig.getLimit(), staticLimit);
      default:
        throw new InvalidArgumentsException("Invalid restriction type");
    }
  }

  private <T> T findImplementationInstance(String className, Class<T> clazz, Injector injector)
      throws ClassNotFoundException {
    Object instance = injector.getInstance(Class.forName(className));

    if (!clazz.isAssignableFrom(instance.getClass())) {
      throw new InvalidArgumentsException(String.format(
          "Configured class [%s] doesn't implement interface [%s]", instance.getClass().getName(), clazz.getName()));
    }
    return (T) instance;
  }

  private void validFeatureInfo(FeatureInfo featureInfo) {
    if (featureInfo.getName() == null) {
      throw new InvalidArgumentsException("Feature is missing name");
    }

    if (featureInfo.getRestrictions() == null) {
      throw new InvalidArgumentsException(
          String.format("Missing mandatory information for feature [%s]", featureInfo.getName()));
    }

    if (!FeatureName.contains(featureInfo.getName())) {
      throw new InvalidArgumentsException(
          String.format("Feature [%s] is not registered in FeatureCollection", featureInfo.getName()));
    }
  }

  private void loadYmlToFeaturesConfig(String ymlPath, List<FeaturesConfig> configs) {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(ymlPath);
    try {
      byte[] bytes = Resources.toByteArray(url);
      FeaturesConfig featuresConfig = om.readValue(bytes, FeaturesConfig.class);
      configs.add(featuresConfig);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to load feature yaml file.", e);
    }
  }
}
