// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package store

import (
	"github.com/wings-software/portal/product/log-service/client"
	"gopkg.in/alecthomas/kingpin.v2"
)

type downloadLinkCommand struct {
	key       string
	accountID string
	server    string
	token     string
}

func (c *downloadLinkCommand) run(*kingpin.ParseContext) error {
	client := client.NewHTTPClient(c.server, c.accountID, c.token, false)
	link, err := client.DownloadLink(nocontext, c.key)
	if err != nil {
		return err
	}
	println(link.Value)
	return nil
}

func registerDownloadLink(app *kingpin.CmdClause) {
	c := new(downloadLinkCommand)

	cmd := app.Command("download-link", "generate a download link").
		Action(c.run)

	cmd.Arg("accountID", "project identifier").
		Required().
		StringVar(&c.accountID)

	cmd.Arg("token", "server token").
		Required().
		StringVar(&c.token)

	cmd.Arg("key", "key identifier").
		Required().
		StringVar(&c.key)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8079").
		StringVar(&c.server)
}
