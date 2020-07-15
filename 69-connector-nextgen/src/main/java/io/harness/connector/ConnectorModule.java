package io.harness.connector;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.connector.impl.ConnectorServiceImpl;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.persistence.HPersistence;

public class ConnectorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ConnectorService.class).to(ConnectorServiceImpl.class);
    registerRequiredBindings();

    MapBinder<String, ConnectionValidator> connectorValidatorMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesConnectionValidator.class);

    MapBinder<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectorDTOToEntityMapper.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GIT.getDisplayName()).to(GitDTOToEntity.class);

    MapBinder<String, ConnectorEntityToDTOMapper> ConnectorEntityToDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, ConnectorEntityToDTOMapper.class);
    ConnectorEntityToDTOMapper.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesEntityToDTO.class);
    ConnectorEntityToDTOMapper.addBinding(ConnectorType.GIT.getDisplayName()).to(GitEntityToDTO.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
