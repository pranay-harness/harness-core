// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package testreports

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

//go:generate mockgen -source reporter.go -package=testreports -destination mocks/reporter.go TestReporter
// TestReporter is any interface which can send us tests in our custom format
type TestReporter interface {
	// Get test case information
	GetTests(context.Context) <-chan *types.TestCase
}
