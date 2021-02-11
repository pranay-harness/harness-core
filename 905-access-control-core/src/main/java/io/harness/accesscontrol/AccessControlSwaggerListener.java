package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.api.PermissionDTO;
import io.harness.accesscontrol.permissions.api.PermissionDTO.PermissionDTOKeys;
import io.harness.accesscontrol.roles.api.RoleDTO;
import io.harness.accesscontrol.roles.api.RoleDTO.RoleDTOKeys;
import io.harness.accesscontrol.scopes.HarnessScope;
import io.harness.accesscontrol.scopes.Scope;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.StringProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SwaggerDefinition
public class AccessControlSwaggerListener implements ReaderListener {
  private final Set<String> scopesAllowedValues;

  public AccessControlSwaggerListener() {
    scopesAllowedValues = Arrays.stream(HarnessScope.values()).map(Scope::getKey).collect(Collectors.toSet());
  }

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {}

  @Override
  public void afterScan(Reader reader, Swagger swagger) {
    ((ArrayProperty) swagger.getDefinitions()
            .get(PermissionDTO.MODEL_NAME)
            .getProperties()
            .get(PermissionDTOKeys.scopes))
        .setItems(new StringProperty()._enum(new ArrayList<>(scopesAllowedValues)));

    ((ArrayProperty) swagger.getDefinitions().get(RoleDTO.MODEL_NAME).getProperties().get(RoleDTOKeys.scopes))
        .setItems(new StringProperty()._enum(new ArrayList<>(scopesAllowedValues)));
  }
}
