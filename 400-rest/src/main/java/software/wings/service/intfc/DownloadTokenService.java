/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public interface DownloadTokenService {
  String createDownloadToken(String resource);
  void validateDownloadToken(String resource, String token);
}
