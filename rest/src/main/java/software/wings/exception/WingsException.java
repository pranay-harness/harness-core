package software.wings.exception;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
public class WingsException extends WingsApiException {
  private static final long serialVersionUID = -3266129015976960503L;

  /**
   * The Response message list.
   */
  private List<ResponseMessage> responseMessageList = new ArrayList<>();

  private Map<String, Object> params = new HashMap<>();

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   */
  public WingsException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message);
  }

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   * @param cause   the cause
   */
  public WingsException(String message, Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, message, cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param cause the cause
   */
  public WingsException(Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, cause);
  }

  /**
   * Instantiates a new Wings exception.
   *
   * @param message the message
   */
  public WingsException(ErrorCode errorCode, String message) {
    this(errorCode, message, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   */
  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param key       the key
   * @param value     the value
   */
  public WingsException(ErrorCode errorCode, String key, Object value) {
    this(errorCode, key, value, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param key       the key
   * @param value     the value
   * @param cause     the cause
   */
  public WingsException(ErrorCode errorCode, String key, Object value, Throwable cause) {
    this(errorCode, cause);
    addParam(key, value);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param cause     the cause
   */
  public WingsException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, errorCode.getCode(), cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param message   the message
   * @param cause     the cause
   */
  public WingsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    responseMessageList.add(ResponseMessage.builder().code(errorCode).message(message).build());
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param params    the params
   * @param errorCode the error code
   */
  public WingsException(Map<String, Object> params, ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
    this.params = params;
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param messageList the message list
   * @param message     the message
   * @param cause       the cause
   */
  public WingsException(@NotEmpty List<ResponseMessage> messageList, String message, Throwable cause) {
    super(message, cause);
    responseMessageList = messageList;
  }

  /**
   * @param messageList
   * @param params
   */
  public WingsException(@NotEmpty List<ResponseMessage> messageList, String message, Map<String, Object> params) {
    super(message, null);
    responseMessageList = messageList;
    this.params = params;
  }

  /**
   * Gets params.
   *
   * @return the params
   */
  public Map<String, Object> getParams() {
    return params;
  }

  /**
   * Sets params.
   *
   * @param params the params
   */
  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  /**
   * Adds the param.
   *
   * @param key   the key
   * @param value the value
   */
  public void addParam(String key, Object value) {
    params.put(key, value);
  }

  /**
   * Gets response message list.
   *
   * @return the response message list
   */
  public List<ResponseMessage> getResponseMessageList() {
    return responseMessageList;
  }

  /**
   * Sets response message list.
   *
   * @param messageList the message list
   */
  public void setResponseMessageList(List<ResponseMessage> messageList) {
    responseMessageList = messageList;
  }
}
