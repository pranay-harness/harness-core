package software.wings.service.impl.azure.delegate;

import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.AutoscaleProfile;
import com.microsoft.azure.management.monitor.AutoscaleSetting;
import com.microsoft.azure.management.monitor.ScaleCapacity;
import com.microsoft.azure.management.monitor.implementation.AutoscaleSettingResourceInner;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import io.fabric8.utils.Objects;
import io.harness.azure.model.AzureConstants;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureAutoScaleSettingsHelperServiceDelegate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureAutoScaleSettingsHelperServiceDelegateImpl
    extends AzureHelperService implements AzureAutoScaleSettingsHelperServiceDelegate {
  @Override
  public Optional<String> getAutoScaleSettingJSONByTargetResourceId(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    Optional<AutoscaleSetting> autoScaleSettingOp =
        getAutoScaleSettingByTargetResourceId(azureConfig, resourceGroupName, targetResourceId);

    if (autoScaleSettingOp.isPresent()) {
      AutoscaleSetting autoscaleSetting = autoScaleSettingOp.get();
      AutoscaleSettingResourceInner autoscaleSettingResourceInner = autoscaleSetting.inner();
      AzureJacksonAdapter adapter = new AzureJacksonAdapter();

      logger.debug("Start serializing AutosScaleSettingResourceInner by resourceGroupName: {}, targetResourceId: {}",
          resourceGroupName, targetResourceId);
      try {
        return Optional.of(adapter.serialize(autoscaleSettingResourceInner));
      } catch (IOException e) {
        throw new InvalidRequestException(
            format("Unable to serialize AutoScaleSetting, resourceGroupName: %s, targetResourceId: %s",
                resourceGroupName, targetResourceId),
            e, USER);
      }
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<AutoscaleSetting> getAutoScaleSettingByTargetResourceId(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }

    if (isBlank(targetResourceId)) {
      throw new IllegalArgumentException(AzureConstants.TARGET_RESOURCE_ID_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing AutosScale settings by resourceGroupName: {}, targetResourceId: {}", resourceGroupName,
        targetResourceId);
    PagedList<AutoscaleSetting> autosScaleSettings = azure.autoscaleSettings().listByResourceGroup(resourceGroupName);
    for (AutoscaleSetting autoScaleSetting : autosScaleSettings) {
      if (autoScaleSetting.autoscaleEnabled() && targetResourceId.equals(autoScaleSetting.targetResourceId())) {
        return Optional.of(autoScaleSetting);
      }
    }
    return Optional.empty();
  }

  @Override
  public void attachAutoScaleSettingToTargetResourceId(AzureConfig azureConfig, final String resourceGroupName,
      final String targetResourceId, final String autoScaleSettingResourceInnerJson,
      ScaleCapacity defaultProfileScaleCapacity) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }

    if (isBlank(targetResourceId)) {
      throw new IllegalArgumentException(AzureConstants.TARGET_RESOURCE_ID_NULL_VALIDATION_MSG);
    }

    if (isBlank(autoScaleSettingResourceInnerJson)) {
      throw new IllegalArgumentException(AzureConstants.AUTOSCALE_SETTINGS_RESOURCE_JSON_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);
    AzureJacksonAdapter adapter = new AzureJacksonAdapter();

    AutoscaleSettingResourceInner autoScaleSettingResourceInner;
    try {
      logger.debug("Start deserialize AutosScaleSettingResourceInner by resourceGroupName: {}, targetResourceId: {}",
          resourceGroupName, targetResourceId);
      autoScaleSettingResourceInner =
          adapter.deserialize(autoScaleSettingResourceInnerJson, AutoscaleSettingResourceInner.class);
    } catch (IOException e) {
      throw new InvalidRequestException(
          format("Unable to deserialize AutoScaleSettingResourceInner, resourceGroupName: %s, "
                  + "targetResourceId: %s, autoScaleSettingResourceInnerJson: %s",
              resourceGroupName, targetResourceId, autoScaleSettingResourceInnerJson),
          e, USER);
    }

    String targetResourceUri = autoScaleSettingResourceInner.targetResourceUri();
    String targetResourceName = autoScaleSettingResourceInner.name();

    if (!targetResourceId.equals(autoScaleSettingResourceInner.targetResourceUri())) {
      throw new InvalidRequestException(
          format("Target resource id doesn't match target resource uri, targetResourceId: %s, "
                  + "targetResourceUri: %s, targetResourceName: %s",
              targetResourceId, targetResourceUri, targetResourceName),
          USER);
    }

    // set min, max and desired capacity on default AutoScaleProfile
    setDefaultAutoScaleProfileCapacity(defaultProfileScaleCapacity, autoScaleSettingResourceInner);

    logger.debug(
        "Start creating or updating AutosScaleSetting by resourceGroupName: {}, targetResourceId: {}, targetResourceName: {}",
        resourceGroupName, targetResourceId, targetResourceName);
    azure.autoscaleSettings().inner().createOrUpdate(
        resourceGroupName, targetResourceName, autoScaleSettingResourceInner);
  }

  private void setDefaultAutoScaleProfileCapacity(
      ScaleCapacity defaultProfileScaleCapacity, AutoscaleSettingResourceInner autoScaleSettingResourceInner) {
    autoScaleSettingResourceInner.profiles().stream().findFirst().ifPresent(
        ap -> ap.withCapacity(defaultProfileScaleCapacity));
  }

  @Override
  public void clearAutoScaleSettingOnTargetResourceId(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    Optional<AutoscaleSetting> autoScaleSettingOp =
        getAutoScaleSettingByTargetResourceId(azureConfig, resourceGroupName, targetResourceId);

    if (autoScaleSettingOp.isPresent()) {
      AutoscaleSetting autoscaleSetting = autoScaleSettingOp.get();

      logger.debug("Start deleting AutosScaleSetting by resourceGroupName: {}, targetResourceId: {}", resourceGroupName,
          targetResourceId);
      azure.autoscaleSettings().deleteById(autoscaleSetting.id());
    } else {
      throw new InvalidRequestException(format("Unable to find AutoScaleSetting, resourceGroupName: %s, "
                                                + "targetResourceId: %s",
                                            resourceGroupName, targetResourceId),
          USER);
    }
  }

  @Override
  public Optional<AutoscaleProfile> getDefaultAutoScaleProfile(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    Optional<AutoscaleSetting> autoScaleSettingOp =
        getAutoScaleSettingByTargetResourceId(azureConfig, resourceGroupName, targetResourceId);

    if (autoScaleSettingOp.isPresent()) {
      AutoscaleSetting autoscaleSetting = autoScaleSettingOp.get();
      return autoscaleSetting.profiles().entrySet().stream().findFirst().map(Map.Entry::getValue);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public List<AutoscaleProfile> listAutoScaleProfilesByTargetResourceId(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    Optional<AutoscaleSetting> autoScaleSettingOp =
        getAutoScaleSettingByTargetResourceId(azureConfig, resourceGroupName, targetResourceId);

    if (autoScaleSettingOp.isPresent()) {
      AutoscaleSetting autoscaleSetting = autoScaleSettingOp.get();
      return new ArrayList<>(autoscaleSetting.profiles().values());
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<String> listAutoScaleProfileJSONsByTargetResourceId(
      AzureConfig azureConfig, final String resourceGroupName, final String targetResourceId) {
    Optional<AutoscaleSetting> autoScaleSettingOp =
        getAutoScaleSettingByTargetResourceId(azureConfig, resourceGroupName, targetResourceId);

    if (autoScaleSettingOp.isPresent()) {
      AutoscaleSetting autoscaleSetting = autoScaleSettingOp.get();
      AutoscaleSettingResourceInner autoScaleSettingInner = autoscaleSetting.inner();
      AzureJacksonAdapter adapter = new AzureJacksonAdapter();

      logger.debug("Start listing AutosScale profiles by resourceGroupName: {}, targetResourceId: {}",
          resourceGroupName, targetResourceId);
      return autoScaleSettingInner.profiles()
          .stream()
          .map(profile -> {
            try {
              return adapter.serialize(profile);
            } catch (IOException e) {
              throw new InvalidRequestException(format("Unable to serialize AutoScaleProfile, resourceGroupName: %s, "
                                                        + "targetResourceId: %s",
                                                    resourceGroupName, targetResourceId),
                  e, USER);
            }
          })
          .collect(Collectors.toList());

    } else {
      return Collections.emptyList();
    }
  }
}
