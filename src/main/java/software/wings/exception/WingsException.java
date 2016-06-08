package software.wings.exception;

import software.wings.beans.ErrorCodes;
import software.wings.beans.ResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;

// TODO: Auto-generated Javadoc

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
public class WingsException extends WebApplicationException {
  private static final long serialVersionUID = -3266129015976960503L;

  /**
   * The Response message list.
   */
  List<ResponseMessage> responseMessageList = new ArrayList<ResponseMessage>();
  private Map<String, Object> params = new HashMap<String, Object>();

  /**
   * Instantiates a new wings exception.
   */
  public WingsException() {
    super();
  }

  public WingsException(String message) {
    super(message);
  }

  public WingsException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param cause the cause
   */
  public WingsException(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   */
  public WingsException(ErrorCodes errorCode) {
    this(errorCode, (Throwable) null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param key       the key
   * @param value     the value
   */
  public WingsException(ErrorCodes errorCode, String key, Object value) {
    this(errorCode, (Throwable) null);
    addParam(key, value);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param cause     the cause
   */
  public WingsException(ErrorCodes errorCode, Throwable cause) {
    this(errorCode, errorCode.getCode(), cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param message   the message
   * @param cause     the cause
   */
  public WingsException(ErrorCodes errorCode, String message, Throwable cause) {
    super(message, cause);
    ResponseMessage responseMessage = new ResponseMessage();
    responseMessage.setCode(errorCode);
    responseMessage.setMessage(message);
    responseMessageList.add(responseMessage);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param params    the params
   * @param errorCode the error code
   */
  public WingsException(Map<String, Object> params, ErrorCodes errorCode) {
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
  public WingsException(List<ResponseMessage> messageList, String message, Throwable cause) {
    super(message, cause);
    this.responseMessageList = messageList;
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
    this.params.put(key, value);
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
    this.responseMessageList = messageList;
  }
}
