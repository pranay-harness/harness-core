// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logger

import (
	"context"
	"net/http"
	"testing"

	"github.com/sirupsen/logrus"
)

func TestContext(t *testing.T) {
	entry := logrus.NewEntry(logrus.StandardLogger())

	ctx := WithContext(context.Background(), entry)
	got := FromContext(ctx)

	if got != entry {
		t.Errorf("Expected Logger from context")
	}
}

func TestEmptyContext(t *testing.T) {
	got := FromContext(context.Background())
	if got != L {
		t.Errorf("Expected default Logger from context")
	}
}

func TestRequest(t *testing.T) {
	entry := logrus.NewEntry(logrus.StandardLogger())

	ctx := WithContext(context.Background(), entry)
	req := new(http.Request)
	req = req.WithContext(ctx)

	got := FromRequest(req)

	if got != entry {
		t.Errorf("Expected Logger from http.Request")
	}
}
