package software.wings.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import software.wings.beans.ResponseMessage;

/**
 *  The generic exception class for the Wings Application.
 *
 *
 * @author Rishi
 *
 */
public class WingsException extends WebApplicationException {
  private Map<String, Object> params = new HashMap<String, Object>();
  List<ResponseMessage> responseMessageList = new ArrayList<ResponseMessage>();

  public WingsException() {
    super();
  }

  public WingsException(Throwable cause) {
    super(cause);
  }
  public WingsException(String errorCode) {
    this(errorCode, null);
  }
  public WingsException(Map<String, Object> params, String errorCode) {
    this(errorCode, null);
    this.params = params;
  }
  public WingsException(String errorCode, Throwable cause) {
    this(errorCode, errorCode, cause);
  }

  public WingsException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    ResponseMessage responseMessage = new ResponseMessage();
    responseMessage.setCode(errorCode);
    responseMessage.setMessage(message);
    responseMessageList.add(responseMessage);
  }

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

  public List<ResponseMessage> getResponseMessageList() {
    return responseMessageList;
  }

  public void setResponseMessageList(List<ResponseMessage> messageList) {
    this.responseMessageList = messageList;
  }
}
