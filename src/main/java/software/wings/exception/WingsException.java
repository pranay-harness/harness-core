package software.wings.exception;

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

  List<ResponseMessage> responseMessageList = new ArrayList<ResponseMessage>();
  private Map<String, Object> params = new HashMap<String, Object>();

  /**
   * Instantiates a new wings exception.
   */
  public WingsException() {
    super();
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
  public WingsException(String errorCode) {
    this(errorCode, null);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param key       the key
   * @param value     the value
   */
  public WingsException(String errorCode, String key, Object value) {
    this(errorCode, null);
    addParam(key, value);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param cause     the cause
   */
  public WingsException(String errorCode, Throwable cause) {
    this(errorCode, errorCode, cause);
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param errorCode the error code
   * @param message   the message
   * @param cause     the cause
   */
  public WingsException(String errorCode, String message, Throwable cause) {
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
  public WingsException(Map<String, Object> params, String errorCode) {
    this(errorCode, null);
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

  public Map<String, Object> getParams() {
    return params;
  }

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

  public List<ResponseMessage> getResponseMessageList() {
    return responseMessageList;
  }

  public void setResponseMessageList(List<ResponseMessage> messageList) {
    this.responseMessageList = messageList;
  }
}
