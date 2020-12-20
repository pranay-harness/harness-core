package io.harness.eventsframework.protohelper;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.encryption.Scope;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;

import com.google.inject.Singleton;
import com.google.protobuf.StringValue;

@Singleton
public class IdentifierRefProtoDTOHelper {
  public IdentifierRefProtoDTO createIdentifierRefProtoDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Scope scope = ScopeHelper.getScope(accountIdentifier, orgIdentifier, projectIdentifier);
    IdentifierRefProtoDTO.Builder identifierRefBuilder = IdentifierRefProtoDTO.newBuilder()
                                                             .setIdentifier(StringValue.of(identifier))
                                                             .setAccountIdentifier(StringValue.of(accountIdentifier))
                                                             .setScope(ScopeProtoEnum.valueOf(scope.toString()));
    if (isNotBlank(orgIdentifier)) {
      identifierRefBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
    }

    if (isNotBlank(projectIdentifier)) {
      identifierRefBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
    }
    return identifierRefBuilder.build();
  }
}
