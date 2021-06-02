package io.harness.yaml.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class YamlValidateMessageMassagerFactory {
  /**
   * In case of library upgrade all the messages will have to be revisited.
   */
  public String massageMessage(ValidationMessage validationMessage) {
    final String code = validationMessage.getCode();
    /**
     * Check error code at {@link ValidatorTypeCode} and also the error msg.
     */
    switch (code) {
      case "1022":
        final String validationMessageArgument = validationMessage.getArguments()[0];
        final int indexOfStartNode = validationMessageArgument.indexOf("{");
        final int indexOfEndNode = validationMessageArgument.lastIndexOf("}");
        final String nodeInMsg = validationMessageArgument.substring(indexOfStartNode, indexOfEndNode + 1);
        String cleanedNodes = nodeInMsg.replaceAll("\"required\":", "");
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(validationMessage.getPath()).append(": ");
        /**
         * Library has hardcoded the msg at {@link
         * com.networknt.schema.OneOfValidator#getMultiSchemasValidErrorMsg(String)}.
         */
        stringBuilder.append("should contain only one of ");
        stringBuilder.append(cleanedNodes);
        return String.valueOf(stringBuilder);
      default:
        return validationMessage.getMessage();
    }
  }
}
