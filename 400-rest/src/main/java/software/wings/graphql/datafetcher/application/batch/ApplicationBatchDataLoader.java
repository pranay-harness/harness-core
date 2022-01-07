/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.application.batch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Application;
import software.wings.graphql.datafetcher.application.ApplicationController;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ApplicationBatchDataLoader implements MappedBatchLoader<String, QLApplication> {
  final AppService appService;

  @Inject
  public ApplicationBatchDataLoader(AppService appService) {
    this.appService = appService;
  }

  @Override
  public CompletionStage<Map<String, QLApplication>> load(Set<String> appIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLApplication> applicationInfoMap = null;
      if (!CollectionUtils.isEmpty(appIds)) {
        applicationInfoMap = getApplicationMap(appIds);
      } else {
        applicationInfoMap = Collections.EMPTY_MAP;
      }
      return applicationInfoMap;
    });
  }

  public Map<String, QLApplication> getApplicationMap(@NotNull Set<String> appIds) {
    List<Application> applicationList = appService.getAppsByIds(appIds);

    return applicationList.stream()
        .map(a -> {
          final QLApplicationBuilder builder = QLApplication.builder();
          ApplicationController.populateQLApplication(a, builder);
          return builder.build();
        })
        .collect(Collectors.toMap(QLApplication::getId, Function.identity()));
  }
}
