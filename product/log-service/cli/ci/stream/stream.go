package cistream

import (
	"context"

	"gopkg.in/alecthomas/kingpin.v2"
)

var nocontext = context.Background()

// Register the stream commands.
func Register(app *kingpin.Application) {
	cmd := app.Command("cistream", "stream commands")
	registerOpen(cmd)
	registerClose(cmd)
	registerPush(cmd)
	registerTail(cmd)
	registerInfo(cmd)
}
