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
