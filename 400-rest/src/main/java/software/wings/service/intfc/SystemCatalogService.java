/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.delegate.beans.FileBucket;
import io.harness.validation.Create;

import software.wings.beans.SystemCatalog;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by sgurubelli on 5/23/17.
 */
public interface SystemCatalogService {
  /**
   * Save
   * @param systemCatalog
   * @param url
   * @param bucket
   * @param size
   * @return
   */
  @ValidationGroups(Create.class)
  SystemCatalog save(
      @Valid SystemCatalog systemCatalog, @NotNull String url, @NotNull FileBucket bucket, @NotNull long size);

  /**
   * List.
   *
   * @param request the request
   * @return the list of System Catalogs
   */
  List<SystemCatalog> list(@NotNull PageRequest<SystemCatalog> request);

  /**
   * Update
   * @param systemCatalog
   * @param url
   * @param bucket
   * @param size
   * @return SystemCatalog
   */
  SystemCatalog update(
      @Valid SystemCatalog systemCatalog, @NotNull String url, @NotNull FileBucket bucket, @NotNull long size);

  /**
   * Get.
   * @param systemCatalogId
   * @return SystemCatalog
   */
  SystemCatalog get(@NotNull String systemCatalogId);
}
