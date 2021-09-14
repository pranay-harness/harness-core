// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package stream

import (
	"context"

	"gopkg.in/alecthomas/kingpin.v2"
)

var nocontext = context.Background()

// Register the stream commands.
func Register(app *kingpin.Application) {
	cmd := app.Command("stream", "stream commands")
	registerOpen(cmd)
	registerClose(cmd)
	registerPush(cmd)
	registerTail(cmd)
	registerInfo(cmd)
}
