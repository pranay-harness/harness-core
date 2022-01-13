/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.exception.InvalidRequestException;

import software.wings.service.impl.aws.model.AwsResponse;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class AwsHelperServiceManager {
  void validateDelegateSuccessForSyncTask(DelegateResponseData notifyResponseData) {
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new InvalidRequestException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage(), USER);
    } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
      throw new InvalidRequestException(
          getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()), USER);
    } else if (!(notifyResponseData instanceof AwsResponse)) {
      throw new InvalidRequestException(
          format("Unknown response from delegate: [%s]", notifyResponseData.getClass().getSimpleName()), USER);
    }
  }
}
