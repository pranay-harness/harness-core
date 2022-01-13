/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public interface DownloadTokenService {
  String createDownloadToken(String resource);
  void validateDownloadToken(String resource, String token);
}
