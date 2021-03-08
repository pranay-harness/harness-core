package io.harness;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.IdentifierRef;
import io.harness.encryption.Scope;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;

import com.google.protobuf.StringValue;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierRefProtoUtils {
  public IdentifierRefProtoDTO createIdentifierRefProtoFromIdentifierRef(IdentifierRef identifierRef) {
    String accountIdentifier = identifierRef.getAccountIdentifier();
    String projectIdentifier = identifierRef.getProjectIdentifier();
    String orgIdentifier = identifierRef.getOrgIdentifier();
    String identifier = identifierRef.getIdentifier();

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
