package io.harness.connector;

import io.harness.aws.AwsClient;
import io.harness.aws.AwsClientImpl;
import io.harness.connector.heartbeat.ConnectorValidationParamsProvider;
import io.harness.connector.impl.ConnectorActivityServiceImpl;
import io.harness.connector.impl.ConnectorFilterServiceImpl;
import io.harness.connector.impl.ConnectorHeartbeatServiceImpl;
import io.harness.connector.impl.DefaultConnectorServiceImpl;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.filter.ConnectorFilterPropertiesMapper;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.ff.FeatureFlagModule;
import io.harness.filter.FilterType;
import io.harness.filter.FiltersModule;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.persistence.HPersistence;
import io.harness.remote.CEAwsSetupConfig;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

public class ConnectorModule extends AbstractModule {
  public static final String DEFAULT_CONNECTOR_SERVICE = "defaultConnectorService";

  private final CEAwsSetupConfig ceAwsSetupConfig;

  public ConnectorModule(CEAwsSetupConfig ceAwsSetupConfig) {
    this.ceAwsSetupConfig = ceAwsSetupConfig;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    install(FiltersModule.getInstance());
    install(FeatureFlagModule.getInstance());

    MapBinder<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, ConnectorEntityToDTOMapper.class);
    MapBinder<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectorDTOToEntityMapper.class);
    MapBinder<String, ConnectionValidator> connectorValidatorMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectionValidator.class);
    MapBinder<String, ConnectorValidationParamsProvider> connectorValidationProviderMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectorValidationParamsProvider.class);

    for (ConnectorType connectorType : ConnectorType.values()) {
      connectorValidatorMapBinder.addBinding(connectorType.getDisplayName())
          .to(ConnectorRegistryFactory.getConnectorValidator(connectorType));
      connectorDTOToEntityMapBinder.addBinding(connectorType.getDisplayName())
          .to(ConnectorRegistryFactory.getConnectorDTOToEntityMapper(connectorType));
      connectorEntityToDTOMapper.addBinding(connectorType.getDisplayName())
          .to(ConnectorRegistryFactory.getConnectorEntityToDTOMapper(connectorType));
      connectorValidationProviderMapBinder.addBinding(connectorType.getDisplayName())
          .to(ConnectorRegistryFactory.getConnectorValidationParamsProvider(connectorType));
    }

    bind(ConnectorService.class)
        .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
        .to(DefaultConnectorServiceImpl.class);
    bind(ConnectorActivityService.class).to(ConnectorActivityServiceImpl.class);
    bind(ConnectorFilterService.class).to(ConnectorFilterServiceImpl.class);
    bind(ConnectorHeartbeatService.class).to(ConnectorHeartbeatServiceImpl.class);
    bind(AwsClient.class).to(AwsClientImpl.class);
    bind(CEAwsSetupConfig.class).toInstance(this.ceAwsSetupConfig);

    MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
        MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
    filterPropertiesMapper.addBinding(FilterType.CONNECTOR.toString()).to(ConnectorFilterPropertiesMapper.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
