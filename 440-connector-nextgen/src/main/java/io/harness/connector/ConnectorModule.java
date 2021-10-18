package io.harness.connector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.aws.AwsClientImpl;
import io.harness.connector.heartbeat.ConnectorValidationParamsProvider;
import io.harness.connector.impl.*;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.filter.ConnectorFilterPropertiesMapper;
import io.harness.connector.services.*;
import io.harness.connector.task.git.GitValidationHandlerViaManager;
import io.harness.connector.task.scm.ScmDelegateClient;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.task.scm.ScmDelegateClientImpl;
import io.harness.filter.FilterType;
import io.harness.filter.FiltersModule;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.persistence.HPersistence;

import static io.harness.annotations.dev.HarnessTeam.DX;

@OwnedBy(DX)
public class ConnectorModule extends AbstractModule {
    private static volatile ConnectorModule instance;
    public static final String DEFAULT_CONNECTOR_SERVICE = "defaultConnectorService";

    private ConnectorModule() {
    }

    public static ConnectorModule getInstance() {
        if (instance == null) {
            instance = new ConnectorModule();
        }

        return instance;
    }

    @Override
    protected void configure() {
        registerRequiredBindings();
        install(FiltersModule.getInstance());

        MapBinder<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapper =
                MapBinder.newMapBinder(binder(), String.class, ConnectorEntityToDTOMapper.class);
        MapBinder<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapBinder =
                MapBinder.newMapBinder(binder(), String.class, ConnectorDTOToEntityMapper.class);
        MapBinder<String, ConnectionValidator> connectorValidatorMapBinder =
                MapBinder.newMapBinder(binder(), String.class, ConnectionValidator.class);
        MapBinder<String, ConnectorValidationParamsProvider> connectorValidationProviderMapBinder =
                MapBinder.newMapBinder(binder(), String.class, ConnectorValidationParamsProvider.class);

        MapBinder<String, ConnectorValidationHandler> connectorValidationHandlerMapBinder =
                MapBinder.newMapBinder(binder(), String.class, ConnectorValidationHandler.class);

        for (ConnectorType connectorType : ConnectorType.values()) {
            connectorValidatorMapBinder.addBinding(connectorType.getDisplayName())
                    .to(ConnectorRegistryFactory.getConnectorValidator(connectorType));
            connectorDTOToEntityMapBinder.addBinding(connectorType.getDisplayName())
                    .to(ConnectorRegistryFactory.getConnectorDTOToEntityMapper(connectorType));
            connectorEntityToDTOMapper.addBinding(connectorType.getDisplayName())
                    .to(ConnectorRegistryFactory.getConnectorEntityToDTOMapper(connectorType));
            connectorValidationProviderMapBinder.addBinding(connectorType.getDisplayName())
                    .to(ConnectorRegistryFactory.getConnectorValidationParamsProvider(connectorType));
            connectorValidationHandlerMapBinder.addBinding(connectorType.getDisplayName()).to(ConnectorRegistryFactory.getConnectorValidationHandler(connectorType));


        }
        bind(ConnectorService.class)
                .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
                .to(DefaultConnectorServiceImpl.class);
        bind(ConnectorActivityService.class).to(ConnectorActivityServiceImpl.class);
        bind(ConnectorFilterService.class).to(ConnectorFilterServiceImpl.class);
        bind(ConnectorHeartbeatService.class).to(ConnectorHeartbeatServiceImpl.class);
        bind(AwsClient.class).to(AwsClientImpl.class);
        bind(ScmDelegateClient.class).to(ScmDelegateClientImpl.class);
        bind(NGConnectorSecretManagerService.class).to(NGConnectorSecretManagerServiceImpl.class);
        MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
                MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
        filterPropertiesMapper.addBinding(FilterType.CONNECTOR.toString()).to(ConnectorFilterPropertiesMapper.class);
//    registerConnectorValidatorsBindings();

    }


    private void registerConnectorValidatorsBindings() {
        MapBinder<String, ConnectorValidationHandler> connectorTypeToConnectorValidationHandlerMap =
                MapBinder.newMapBinder(binder(), String.class, ConnectorValidationHandler.class);
        connectorTypeToConnectorValidationHandlerMap.addBinding(ConnectorType.GIT.getDisplayName())
                .to(GitValidationHandlerViaManager.class);
    }

    private void registerRequiredBindings() {
        requireBinding(HPersistence.class);
    }
}