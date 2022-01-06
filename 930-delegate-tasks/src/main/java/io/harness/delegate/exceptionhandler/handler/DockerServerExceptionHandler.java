/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.exception.runtime.DockerHubInvalidImageRuntimeRuntimeException;
import io.harness.exception.runtime.DockerHubInvalidTagRuntimeRuntimeException;
import io.harness.exception.runtime.DockerHubServerRuntimeException;
import io.harness.exception.runtime.InvalidDockerHubCredentialsRuntimeException;
import io.harness.manage.GlobalContextManager;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class DockerServerExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(DockerHubServerRuntimeException.class).build();
  }
  @Override
  public WingsException handleException(Exception exception) {
    DockerHubServerRuntimeException ex = (DockerHubServerRuntimeException) exception;
    if (ex instanceof InvalidDockerHubCredentialsRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.REGISTRY_ACCESS_DENIED,
                                            imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
            new HintException(String.format(HintException.HINT_INVALID_CONNECTOR,
                                  imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new InvalidCredentialsException(ex.getMessage(), USER)));
      }
      return new HintException(
          HintException.HINT_DOCKER_HUB_ACCESS_DENIED, new InvalidCredentialsException(exception.getMessage(), USER));
    } else if (ex instanceof DockerHubInvalidImageRuntimeRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.COMMAND_TRIED_FOR_ARTIFACT,
                                            imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ExplanationException(String.format(ExplanationException.IMAGE_METADATA_NOT_FOUND,
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                         imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new HintException(HintException.HINT_INVALID_IMAGE_REFER_LINK_DOCKER_HUB,
                    new io.harness.exception.ImageNotFoundException(ex.getMessage(), USER))));
      }
      return new HintException(
          HintException.HINT_DOCKER_HUB_IMAGE_NAME, new ImageNotFoundException(exception.getMessage(), USER));
    } else if (ex instanceof DockerHubInvalidTagRuntimeRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.COMMAND_TRIED_FOR_ARTIFACT,
                                            imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ExplanationException(String.format(ExplanationException.IMAGE_TAG_METADATA_NOT_FOUND,
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_TAG.name()),
                                         imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_DOCKER_HUB,
                    new io.harness.exception.InvalidTagException(ex.getMessage(), USER))));
      }
      return new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_DOCKER_HUB,
          new io.harness.exception.InvalidTagException(ex.getMessage(), USER));
    } else {
      return new InvalidRequestException("Could not get build details");
    }
  }
}
