/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.utils;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;

import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.features.api.Usage;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

public class ServiceUtils {
  private ServiceUtils() {
    throw new AssertionError();
  }

  public static boolean isTemplateLibraryUsed(Service service) {
    return service != null && isNotEmpty(service.getServiceCommands())
        && service.getServiceCommands().stream().anyMatch(sc -> StringUtils.isNotBlank(sc.getTemplateUuid()));
  }

  public static List<Service> getServicesWithTemplateLibrary(List<Service> serviceList) {
    return serviceList.stream().filter(ServiceUtils::isTemplateLibraryUsed).collect(Collectors.toList());
  }

  public static PageRequest<Service> getServicesPageRequest(@NotNull String accountId, List<String> serviceIdList) {
    PageRequestBuilder pageRequestBuilder =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(Service.ACCOUNT_ID_KEY2, Operator.EQ, accountId);

    if (isNotEmpty(serviceIdList)) {
      pageRequestBuilder.addFilter(Service.ID, Operator.IN, serviceIdList.toArray());
    }

    return (PageRequest<Service>) pageRequestBuilder.build();
  }

  public static Usage toUsage(Service service) {
    return Usage.builder()
        .entityId(service.getUuid())
        .entityName(service.getName())
        .entityType(EntityType.SERVICE.name())
        .property(ServiceKeys.appId, service.getAppId())
        .build();
  }
}
