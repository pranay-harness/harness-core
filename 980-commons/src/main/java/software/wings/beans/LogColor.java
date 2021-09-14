/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

public enum LogColor {
  Red(91),
  Orange(33),
  Yellow(93),
  Green(92),
  Blue(94),
  Purple(95),
  Cyan(96),
  Gray(37),
  Black(30),
  White(97),

  GrayDark(90),
  RedDark(31),
  GreenDark(32),
  BlueDark(34),
  PurpleDark(35),
  CyanDark(36);

  final int value;

  LogColor(int value) {
    this.value = value;
  }
}
