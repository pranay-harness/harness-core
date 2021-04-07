package io.harness.resourcegroup.resourceclient.audit;

import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.util.stream.Collectors.toList;

import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.Scope;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class AuditResourceValidatorImpl implements ResourceValidator {
  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public EnumSet<ValidatorType> getValidatorTypes() {
    return EnumSet.of(DYNAMIC);
  }

  @Override
  public String getResourceType() {
    return "AUDIT";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.empty();
  }

  @Override
  public ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message) {
    return null;
  }
}
