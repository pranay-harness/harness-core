package store

import (
	"context"

	"gopkg.in/alecthomas/kingpin.v2"
)

var nocontext = context.Background()

// Register the store commands.
func Register(app *kingpin.Application) {
	cmd := app.Command("store", "storage commands")
	registerDownload(cmd)
	registerDownloadLink(cmd)
	registerUpload(cmd)
	registerUploadLink(cmd)
}
