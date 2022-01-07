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
import io.harness.exception.ConnectException;
import io.harness.exception.ExplanationException;
import io.harness.exception.GcpServerException;
import io.harness.exception.HintException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.exception.runtime.GcpClientRuntimeException;
import io.harness.exception.runtime.GcrConnectRuntimeException;
import io.harness.exception.runtime.GcrImageNotFoundRuntimeException;
import io.harness.exception.runtime.GcrInvalidTagRuntimeException;
import io.harness.manage.GlobalContextManager;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class GcpClientExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(GcpClientRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    GcpClientRuntimeException ex = (GcpClientRuntimeException) exception;
    if (ex instanceof GcrInvalidTagRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.COMMAND_TRIED_FOR_ARTIFACT,
                                            imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ExplanationException(String.format(ExplanationException.IMAGE_TAG_METADATA_NOT_FOUND,
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_TAG.name()),
                                         imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_GCR,
                    new io.harness.exception.InvalidTagException(ex.getMessage(), USER))));
      }
      return new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_GCR,
          new io.harness.exception.InvalidTagException(ex.getMessage(), USER));
    } else if (ex instanceof GcrImageNotFoundRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.COMMAND_TRIED_FOR_ARTIFACT,
                                            imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ExplanationException(String.format(ExplanationException.IMAGE_METADATA_NOT_FOUND,
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                         imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new HintException(HintException.HINT_INVALID_IMAGE_REFER_LINK_GCR,
                    new io.harness.exception.ImageNotFoundException(ex.getMessage(), USER))));
      }
      return new HintException(HintException.HINT_GCR_IMAGE_NAME, new ImageNotFoundException(ex.getMessage(), USER));
    } else if (ex instanceof GcrConnectRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new HintException(
            String.format(HintException.HINT_HOST_UNREACHABLE, imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ConnectException(ex.getMessage(), USER));
      }
      return new HintException(HintException.HINT_HOST_UNREACHABLE, new ConnectException(ex.getMessage(), USER));
    } else if (ex instanceof GcpClientRuntimeException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.REGISTRY_ACCESS_DENIED,
                                            imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
            new HintException(String.format(HintException.HINT_INVALID_CONNECTOR,
                                  imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new InvalidCredentialsException(ex.getMessage(), USER)));
      }
      return new HintException(HintException.HINT_GCP_ACCESS_DENIED,
          new HintException(
              "Could not get basic auth header", new GcpServerException(ex.getMessage(), WingsException.USER)));
    } else {
      return new GcpServerException("Could not get basic auth header");
    }
  }
}
