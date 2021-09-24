package io.harness.app.schema.mutation.template.payload;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.Template;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@OwnedBy(PL)
@Scope(PermissionAttribute.ResourceType.TEMPLATE)
public class QLTemplateListPayload implements QLMutationPayload {
  String clientMutationId;
  List<Template> nodes;
}
