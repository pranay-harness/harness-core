package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.billing.graphql.GcpOrganizationQueryArguments;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationDTO;
import io.harness.ccm.config.GcpOrganizationService;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GcpOrganizationDataFetcher
    extends AbstractArrayDataFetcher<GcpOrganizationDTO, GcpOrganizationQueryArguments> {
  @Inject GcpOrganizationService gcpOrganizationService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected List<GcpOrganizationDTO> fetch(GcpOrganizationQueryArguments arguments, String accountId) {
    String uuid = arguments.getUuid();
    List<GcpOrganization> gcpOrganizations;
    if (isNotEmpty(uuid)) {
      gcpOrganizations = Collections.singletonList(gcpOrganizationService.get(uuid));
    } else {
      gcpOrganizations = gcpOrganizationService.list(accountId);
    }
    return gcpOrganizations.stream()
        .map(gcpOrganization
            -> GcpOrganizationDTO.builder()
                   .uuid(gcpOrganization.getUuid())
                   .accountId(gcpOrganization.getAccountId())
                   .organizationId(gcpOrganization.getOrganizationId())
                   .organizationName(gcpOrganization.getOrganizationName())
                   .serviceAccount(gcpOrganization.getServiceAccountEmail())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  protected GcpOrganizationDTO unusedReturnTypePassingDummyMethod() {
    return null;
  }
}