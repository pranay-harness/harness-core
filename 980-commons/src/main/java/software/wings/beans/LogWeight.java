package software.wings.beans;

public enum LogWeight {
  Normal(0),
  Bold(1);

  final int value;

  LogWeight(int value) {
    this.value = value;
  }
}
