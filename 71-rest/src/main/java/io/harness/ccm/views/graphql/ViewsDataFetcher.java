package io.harness.ccm.views.graphql;

import com.google.inject.Inject;

import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.service.CEViewService;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ViewsDataFetcher extends AbstractObjectDataFetcher<QLCEViewsData, QLNoOpQueryParameters> {
  @Inject private CEViewService viewService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEViewsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    Map<ViewType, List<QLCEView>> viewTypeListMap =
        viewService.getAllViews(accountId).stream().collect(Collectors.groupingBy(view -> view.getViewType()));
    return QLCEViewsData.builder()
        .sampleViews(viewTypeListMap.get(ViewType.SAMPLE))
        .customerViews(viewTypeListMap.get(ViewType.CUSTOMER))
        .build();
  }
}