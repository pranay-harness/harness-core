package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.Service;
import software.wings.licensing.violations.RestrictedFeature;

import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

@Singleton
public interface ServiceViolationCheckerMixin {
  Collection<RestrictedFeature> SERVICE_RESTRICTED_FEATURES = ImmutableList.of(RestrictedFeature.TEMPLATE_LIBRARY);

  default List<Usage> getViolationsInServices(@NotNull List<Service> serviceList) {
    List<Usage> templateLibraryUsages = Lists.newArrayList();
    serviceList.stream().filter(s -> isNotEmpty(s.getServiceCommands())).forEach(s -> {
      boolean usesTemplateLibrary =
          s.getServiceCommands().stream().anyMatch(sc -> StringUtils.isNotBlank(sc.getTemplateUuid()));
      if (usesTemplateLibrary) {
        templateLibraryUsages.add(Usage.builder()
                                      .entityId(s.getUuid())
                                      .entityName(s.getName())
                                      .entityType(EntityType.SERVICE.name())
                                      .property(Service.APP_ID_KEY, s.getAppId())
                                      .build());
      }
    });
    return templateLibraryUsages;
  }

  default PageRequest<Service> getServicePageRequest(@NotNull String accountId, List<String> serviceIdList) {
    PageRequestBuilder pageRequestBuilder =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(Service.ACCOUNT_ID_KEY, Operator.EQ, accountId);

    if (isNotEmpty(serviceIdList)) {
      pageRequestBuilder.addFilter(Service.ID_KEY, Operator.IN, serviceIdList.toArray());
    }

    return pageRequestBuilder.build();
  }
}
