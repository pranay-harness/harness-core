/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
