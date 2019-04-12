package software.wings.graphql.datafetcher.application.batchloader;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.beans.Application.APP_ID_KEY;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.application.ApplicationAdaptor;
import software.wings.graphql.schema.type.ApplicationInfo;
import software.wings.service.intfc.AppService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationBatchDataLoader implements MappedBatchLoader<String, ApplicationInfo> {
  AppService appService;
  ApplicationAdaptor applicationAdaptor;

  @Inject
  public ApplicationBatchDataLoader(AppService appService, ApplicationAdaptor applicationAdaptor) {
    this.appService = appService;
    this.applicationAdaptor = applicationAdaptor;
  }

  @Override
  public CompletionStage<Map<String, ApplicationInfo>> load(Set<String> appIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, ApplicationInfo> applicationInfoMap;
      if (!CollectionUtils.isEmpty(appIds)) {
        PageRequest<Application> pageRequest =
            aPageRequest().addFilter(APP_ID_KEY, Operator.IN, appIds.toArray()).build();
        PageResponse<Application> applications = appService.list(pageRequest, false);

        applicationInfoMap = applications.getResponse()
                                 .stream()
                                 .map(a -> applicationAdaptor.getApplicationInfo(a))
                                 .collect(Collectors.toMap(ApplicationInfo::getId, Function.identity()));
      } else {
        applicationInfoMap = Collections.EMPTY_MAP;
      }

      return applicationInfoMap;
    });
  }
}
