package io.harness.checks;

public class UseIsEmptyCheckIssues {
  void func() {
    List<Integer> list = null;

    if (list == null || list.isEmpty()) {
    }
    if (null == list || list.isEmpty()) {
    }

    if (list != null && !list.isEmpty()) {
    }
    if (null != list && !list.isEmpty()) {
    }
    if (list != null && list.isNotEmpty()) {
    }
    if (null != list && list.isNotEmpty()) {
    }
  }
}
