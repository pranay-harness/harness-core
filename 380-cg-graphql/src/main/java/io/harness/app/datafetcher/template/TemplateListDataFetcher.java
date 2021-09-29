package io.harness.app.datafetcher.template;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SearchFilter.Operator.IN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.template.input.QLTemplateListInput;
import io.harness.app.schema.mutation.template.payload.QLTemplateListPayload;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateKeys;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(PL)

public class TemplateListDataFetcher extends BaseMutatorDataFetcher<QLTemplateListInput, QLTemplateListPayload> {
  @Inject public TemplateService templateService;

  @Inject
  TemplateListDataFetcher(TemplateService templateService) {
    super(QLTemplateListInput.class, QLTemplateListPayload.class);
    this.templateService = templateService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT)
  public QLTemplateListPayload mutateAndFetch(QLTemplateListInput parameter, MutationContext mutationContext) {
    String accountId = parameter.getAccountId();
    List<String> galleryKeys = new ArrayList<>();
    String limit = parameter.getLimit();
    String offset = parameter.getOffset();
    PageRequest<Template> pageRequest = new PageRequest<>();
    pageRequest.addFilter(TemplateKeys.accountId, IN, accountId);
    pageRequest.setLimit(limit);
    pageRequest.setOffset(offset);
    PageResponse<Template> pageResponse = templateService.list(pageRequest, galleryKeys, accountId, false);
    List<Template> nodes = pageResponse.getResponse();
    return new QLTemplateListPayload(parameter.getClientMutationId(), nodes);
  }
}