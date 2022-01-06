// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package cli

import (
	"context"
	"os"

	"github.com/wings-software/portal/product/log-service/cli/server"
	"github.com/wings-software/portal/product/log-service/cli/store"
	"github.com/wings-software/portal/product/log-service/cli/stream"

	"gopkg.in/alecthomas/kingpin.v2"
)

// program version
var version = "0.0.0"

// empty context
var nocontext = context.Background()

// Command parses the command line arguments and then executes a
// subcommand program.
func Command() {
	app := kingpin.New("harness-logger", "harness logging service")

	server.Register(app)
	store.Register(app)
	stream.Register(app)

	kingpin.Version(version)
	kingpin.MustParse(app.Parse(os.Args[1:]))
}
