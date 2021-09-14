// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

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
