package io.harness.pcf.command;

import static io.harness.exception.WingsException.USER;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.ReflectionException;
import io.harness.pcf.command.option.Flag;
import io.harness.pcf.command.option.LoginOptions;
import io.harness.pcf.command.option.Option;
import io.harness.pcf.command.option.Options;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class CfCliCommandTemplateResolver {
  private static final String OPTION_TEMPLATE = "${KEY} ${VALUE}";

  private CfCliCommandTemplateResolver() {}

  public static String getCliVersionCommand(CommandArguments commandArguments) {
    return CfCliCommandTemplateFactory.getCfCliCommandTemplate(CfCliCommandType.VERSION, commandArguments.getCliPath());
  }

  public static String getCliLoginCommand(CommandArguments commandArguments, LoginOptions options) {
    validateCommandArguments(commandArguments);
    String cliCommand =
        CfCliCommandTemplateFactory.getCfCliCommandTemplate(CfCliCommandType.LOGIN, commandArguments.getCliPath());

    return cliCommand.replace("${OPTIONS}", buildCommandOptions(options));
  }

  private static void validateCommandArguments(CommandArguments commandArguments) {
    if (commandArguments == null) {
      throw new InvalidArgumentsException("Parameter commandArguments cannot be null");
    }

    if (commandArguments.getCliVersion() == null) {
      throw new InvalidArgumentsException("Parameter cliVersion cannot be null");
    }
  }

  private static String buildCommandOptions(Options optionsData) {
    Class<? extends Options> clazz = optionsData.getClass();
    Field[] declaredFields = clazz.getDeclaredFields();
    List<String> optionList = new ArrayList<>();
    for (Field field : declaredFields) {
      try {
        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        String optionKey = getOptionAnnotationValue(field);
        if (optionKey != null) {
          Object optionFieldValue = field.get(optionsData);
          if (fieldType == String.class) {
            addOption(optionList, optionKey, (String) optionFieldValue);
          }
        }

        String flagKey = getFlagAnnotationValue(field);
        if (flagKey != null) {
          Object flagFieldValue = field.get(optionsData);
          if (fieldType == boolean.class) {
            addFlag(optionList, flagKey, (boolean) flagFieldValue);
          }
        }
      } catch (IllegalAccessException e) {
        throw new ReflectionException("Illegal access for field value", e, ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION,
            Level.ERROR, USER, EnumSet.of(FailureType.APPLICATION_ERROR));
      }
    }

    return String.join(SPACE, optionList);
  }

  private static String getOptionAnnotationValue(Field field) {
    if (field == null) {
      return null;
    }

    Option option = field.getAnnotation(Option.class);
    return option != null ? option.value() : null;
  }

  private static String getFlagAnnotationValue(Field field) {
    if (field == null) {
      return null;
    }

    Flag flag = field.getAnnotation(Flag.class);
    return flag != null ? flag.value() : null;
  }

  private static void addOption(List<String> optionList, final String optionKey, final String fieldValue) {
    if (isNotBlank(fieldValue)) {
      optionList.add(OPTION_TEMPLATE.replace("${KEY}", optionKey).replace("${VALUE}", fieldValue));
    }
  }

  private static void addFlag(List<String> optionList, final String flag, boolean fieldValue) {
    if (fieldValue) {
      optionList.add(flag);
    }
  }
}
