/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.winrm;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.SSL_HANDSHAKE_FAILED;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.eraro.ResponseMessage.ResponseMessageBuilder;

import static java.lang.String.format;

import io.harness.eraro.ResponseMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import javax.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.transport.http.HTTPException;

public class WinRmHelperUtils {
  public static ResponseMessage buildErrorDetailsFromWinRmClientException(Throwable e) {
    ResponseMessageBuilder builder = ResponseMessage.builder().code(UNKNOWN_ERROR).message("Generic Error");
    Throwable e1 = e;

    while (e1 != null) {
      if (e1 instanceof InvocationTargetException) {
        e1 = ((InvocationTargetException) e1).getTargetException();
        continue;
      }
      if (e1 instanceof UndeclaredThrowableException) {
        e1 = ((UndeclaredThrowableException) e1).getUndeclaredThrowable();
        continue;
      }
      if (e1 instanceof java.net.ConnectException) {
        builder.code(UNREACHABLE_HOST);
        builder.message(format("Cannot reach remote host. Details: %s", e1.getMessage()));
        break;
      } else if (e1 instanceof javax.net.ssl.SSLHandshakeException) {
        builder.code(SSL_HANDSHAKE_FAILED);
        builder.message(
            "Error in SSL negotiation. Check if server certificate is correct. You may try with skipCertChecks");
        break;
      } else if (e1 instanceof SOAPFaultException) {
        builder.message(e1.getMessage());
        break;
      } else if (e1 instanceof java.io.IOException) {
        if (e1.getMessage().contains("Authorization")) {
          builder.code(INVALID_CREDENTIAL);
          builder.message(
              "Authorization Error: Invalid credentials. Check AuthenticationScheme, username and password");
        } else {
          builder.message(e1.getMessage());
        }
        break;
      } else if (e1 instanceof HTTPException) {
        HTTPException httpException = (HTTPException) e1;
        if (httpException.getResponseCode() == 401 || httpException.getResponseCode() == 403) {
          builder.code(INVALID_CREDENTIAL);
        }
        builder.message(e1.getMessage());
      }
      e1 = e1.getCause();
    }
    return builder.build();
  }
}
