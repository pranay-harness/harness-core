package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

@Slf4j
public class TemplateLibraryViolationChecker implements FeatureViolationChecker {
  private ServiceResourceService serviceResourceService;
  private HPersistence hPersistence;

  @Inject
  public TemplateLibraryViolationChecker(ServiceResourceService serviceResourceService, HPersistence hPersistence) {
    this.serviceResourceService = serviceResourceService;
    this.hPersistence = hPersistence;
  }

  public List<FeatureViolation> getViolationsForCommunityAccount(@NotNull String accountId) {
    List<FeatureViolation> featureViolationList = null;

    List<Usage> usages = Stream.of(getViolationsInWorkflows(accountId), getViolationsInServices(accountId))
                             .flatMap(Collection::stream)
                             .collect(Collectors.toList());

    if (isNotEmpty(usages)) {
      featureViolationList = Collections.singletonList(
          FeatureUsageViolation.builder().restrictedFeature(RestrictedFeature.TEMPLATE_LIBRARY).usages(usages).build());
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  private List<Usage> getViolationsInWorkflows(String accountId) {
    final Query<Workflow> query = hPersistence.createQuery(Workflow.class).filter(WorkflowKeys.accountId, accountId);
    List<Usage> templateLibraryUsages = Lists.newArrayList();
    try (HIterator<Workflow> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        Workflow workflow = iterator.next();
        if (isNotEmpty(workflow.getLinkedTemplateUuids())) {
          templateLibraryUsages.add(Usage.builder()
                                        .entityId(workflow.getUuid())
                                        .entityName(workflow.getName())
                                        .entityType(EntityType.WORKFLOW.name())
                                        .build());
        }
      }
    }
    return templateLibraryUsages;
  }

  private List<Usage> getViolationsInServices(String accountId) {
    List<Usage> templateLibraryUsages = Lists.newArrayList();

    PageRequest<Service> pageRequest = aPageRequest()
                                           .withLimit(PageRequest.UNLIMITED)
                                           .addFilter(Service.ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                           .build();

    PageResponse<Service> servicePageResponse = serviceResourceService.list(pageRequest, false, true);

    servicePageResponse.stream().filter(s -> isNotEmpty(s.getServiceCommands())).forEach(s -> {
      boolean usesTemplateLibrary =
          s.getServiceCommands().stream().anyMatch(sc -> StringUtils.isNotBlank(sc.getTemplateUuid()));
      if (usesTemplateLibrary) {
        templateLibraryUsages.add(Usage.builder()
                                      .entityId(s.getUuid())
                                      .entityName(s.getName())
                                      .entityType(EntityType.SERVICE.name())
                                      .build());
      }
    });
    return templateLibraryUsages;
  }
}
