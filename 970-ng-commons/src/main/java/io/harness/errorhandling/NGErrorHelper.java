/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.errorhandling;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.UnexpectedException;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.dto.ErrorMessageInfo;

import com.google.inject.Singleton;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NGErrorHelper {
  public static int DEFAULT_ERROR_CODE = 450;
  public static String DEFAULT_ERROR_SUMMARY = "Unexpected Error";
  public static String DEFAULT_ERROR_MESSAGE = "Something went wrong on our end. Please contact Harness Support.";
  public static String DEFAULT_REASON = "Unexpected Error";
  private static Map<Integer, ErrorMessageInfo> errorCodeToErrorMap = new HashMap<>();
  private static Map<String, ErrorMessageInfo> errorMessageToErrorMap = new HashMap<>();
  private static boolean isInitialized;
  public static final String ERROR_MESSAGE_FILE = "/error_messages.properties";
  private static final String DONT_OVERRIDE_STRING = "DONT_OVERRIDE";

  public static void intializeErrorMessageMap() {
    if (!isInitialized) {
      readErrorsAndPopulateTheErrorMaps();
      isInitialized = true;
    }
  }

  private static void readErrorsAndPopulateTheErrorMaps() {
    try {
      Properties errors = new Properties();
      InputStream inputStream = NGErrorHelper.class.getResourceAsStream(ERROR_MESSAGE_FILE);
      errors.load(inputStream);
      errors.forEach(NGErrorHelper::populateErrorInMap);
    } catch (Exception ex) {
      throw new UnexpectedException("Could not read the error file");
    }
  }

  private static void populateErrorInMap(Object key, Object value) {
    try {
      Integer errorCode = Integer.valueOf((String) key);
      String errorDetailString = (String) value;

      // Splitting the error details to separate out the error message and reason
      String[] errorDetailsList = errorDetailString.split(",");
      String errorMessage = errorDetailsList[0];
      String errorReason = errorDetailsList[1];
      String errorCategory = errorDetailsList[2];
      String ovverideMessage = null;
      if (errorDetailsList.length == 4) {
        ovverideMessage = errorDetailsList[3];
      }
      ErrorMessageInfo errorDetail = ErrorMessageInfo.builder()
                                         .code(errorCode)
                                         .messageRegex(errorMessage)
                                         .reason(errorReason)
                                         .errorCategory(errorCategory)
                                         .overriddenMessage(ovverideMessage)
                                         .build();
      errorCodeToErrorMap.put(errorCode, errorDetail);
      errorMessageToErrorMap.put(errorMessage, errorDetail);
    } catch (Exception ex) {
      log.info("Exception encountered while processing the error with the error code [{}]", key);
    }
  }

  public ErrorDetail createErrorDetail(String errorMessage) {
    intializeErrorMessageMap();
    if (isNotBlank(errorMessage)) {
      for (String errorRegex : errorMessageToErrorMap.keySet()) {
        Pattern errorPattern = getPattern(errorRegex);
        if (errorPattern.matcher(errorMessage).matches()) {
          ErrorMessageInfo savedErrorDetail = errorMessageToErrorMap.get(errorRegex);
          return ErrorDetail.builder()
              .reason(savedErrorDetail.getReason())
              .message(getOveriddenMessageOrActualMessage(errorMessage, savedErrorDetail.getOverriddenMessage()))
              .code(savedErrorDetail.getCode())
              .build();
        }
      }
    }
    if (isBlank(errorMessage)) {
      errorMessage = DEFAULT_ERROR_MESSAGE;
    }
    return ErrorDetail.builder().reason(DEFAULT_REASON).code(DEFAULT_ERROR_CODE).message(errorMessage).build();
  }

  private Pattern getPattern(String regex) {
    return Pattern.compile(regex, Pattern.DOTALL);
  }

  public int getCode(String errorMessage) {
    intializeErrorMessageMap();
    if (isNotBlank(errorMessage)) {
      for (String errorRegex : errorMessageToErrorMap.keySet()) {
        Pattern errorPattern = getPattern(errorRegex);
        if (errorPattern.matcher(errorMessage).matches()) {
          ErrorMessageInfo savedErrorDetail = errorMessageToErrorMap.get(errorRegex);
          return savedErrorDetail.getCode();
        }
      }
    }
    return DEFAULT_ERROR_CODE;
  }

  public String getReason(String errorMessage) {
    intializeErrorMessageMap();
    if (isNotBlank(errorMessage)) {
      for (String errorRegex : errorMessageToErrorMap.keySet()) {
        Pattern errorPattern = getPattern(errorRegex);
        if (errorPattern.matcher(errorMessage).matches()) {
          ErrorMessageInfo savedErrorDetail = errorMessageToErrorMap.get(errorRegex);
          return savedErrorDetail.getReason();
        }
      }
    }
    return DEFAULT_REASON;
  }

  public String getErrorSummary(String errorMessage) {
    intializeErrorMessageMap();
    if (isNotBlank(errorMessage)) {
      for (String errorRegex : errorMessageToErrorMap.keySet()) {
        Pattern errorPattern = getPattern(errorRegex);
        if (errorPattern.matcher(errorMessage).matches()) {
          ErrorMessageInfo savedErrorDetail = errorMessageToErrorMap.get(errorRegex);
          return String.format("%s (%s)", savedErrorDetail.getErrorCategory(),
              getOveriddenMessageOrActualMessage(errorMessage, savedErrorDetail.getOverriddenMessage()));
        }
      }
    }
    if (isBlank(errorMessage)) {
      errorMessage = DEFAULT_ERROR_MESSAGE;
    }
    return String.format("%s (%s)", "Error Encountered", errorMessage);
  }

  private String getOveriddenMessageOrActualMessage(String errorMessage, String overriddenMessage) {
    if (overriddenMessage == null) {
      return errorMessage;
    }
    return overriddenMessage;
  }

  public String createErrorSummary(String errorCategory, String errorMessage) {
    return String.format("%s (%s)", errorCategory, errorMessage);
  }

  public ErrorDetail getGenericErrorDetail() {
    return ErrorDetail.builder().code(DEFAULT_ERROR_CODE).reason(DEFAULT_REASON).message(DEFAULT_ERROR_MESSAGE).build();
  }
}
