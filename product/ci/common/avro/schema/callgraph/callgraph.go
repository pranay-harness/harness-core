package schema

import (
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"strings"
)

func bindata_read(data []byte, name string) ([]byte, error) {
	gz, err := gzip.NewReader(bytes.NewBuffer(data))
	if err != nil {
		return nil, fmt.Errorf("Read %q: %v", name, err)
	}

	var buf bytes.Buffer
	_, err = io.Copy(&buf, gz)
	gz.Close()

	if err != nil {
		return nil, fmt.Errorf("Read %q: %v", name, err)
	}

	return buf.Bytes(), nil
}

var _callgraph_avsc = []byte("\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\xff\xe4\x93\xc1\x6e\xf3\x20\x0c\xc7\xef\x7d\x0a\xe4\x73\xd5\x07\xc8\xf5\x7b\x81\x4f\xbd\x4e\x3d\x78\xe0\x26\xd6\x08\x44\x98\x4d\xaa\xa2\xbe\xfb\x44\x52\xba\x90\xe4\x50\xa5\xda\x0e\x1b\x27\x30\xfe\xff\x7f\x60\x43\xbf\x53\x4a\x29\x70\xd8\x12\x54\x0a\xfe\x63\x20\x17\x61\x3f\x46\xe3\xa5\x23\xa8\x20\x90\xf6\xc1\xe4\x60\x4a\x95\x0e\x35\x81\xaa\x14\x44\x3e\x34\x18\x1c\x89\x1c\xd8\xe7\x94\x33\x93\x35\x02\xd5\xcb\xb0\x4c\xa3\xbf\xcf\x26\x38\x70\xde\x90\xdc\x44\xf7\xbd\x11\x5a\x0a\x26\x1b\x0a\x30\x04\xbc\xc0\x5e\xa9\x65\x0a\x47\x6a\x65\x4d\x3c\xa5\xfe\x6b\xd8\x9a\x19\x75\x06\x29\xaf\xbc\x48\x5a\xde\x6f\x3e\xfa\x11\x96\x2a\xd4\x52\x6c\xbc\x81\xfd\xcd\x3b\x85\x24\x06\x76\x35\x5c\xd7\xed\x67\xfa\x0e\xf5\x1b\xd6\xb4\xdd\x80\x4b\x38\xbb\xf8\x30\x39\x60\x2b\xdb\xc1\xda\xa2\x3c\x21\x1f\x54\x9b\xd4\x89\x8d\xd6\xca\x91\xce\x96\x74\x64\xef\x0a\x9f\x57\xef\x2d\xa1\x7b\xcc\xe8\xcc\x76\xfd\x14\xab\xda\xd3\x22\x5a\xe6\x7d\xad\x26\xf0\xf5\xcf\x11\x49\xe2\x91\x2c\xa6\xe3\xff\xfa\x4f\x22\xfe\x3d\x68\xda\xf8\x4e\x53\xa5\x8a\x77\xd6\xe7\x13\xe6\x2a\xe4\x3b\x8f\xa6\xdf\xde\xba\x0f\x96\x3a\x60\xd7\x3c\xdd\xbe\x3f\xd0\x3c\x43\x12\xd9\xe5\x32\xfd\x5c\x0f\x87\xd9\x49\xed\xae\xbb\xcf\x00\x00\x00\xff\xff\xcc\x37\x03\x2b\x03\x07\x00\x00")

func callgraph_avsc() ([]byte, error) {
	return bindata_read(
		_callgraph_avsc,
		"callgraph.avsc",
	)
}

// Asset loads and returns the asset for the given name.
// It returns an error if the asset could not be found or
// could not be loaded.
func Asset(name string) ([]byte, error) {
	cannonicalName := strings.Replace(name, "\\", "/", -1)
	if f, ok := _bindata[cannonicalName]; ok {
		return f()
	}
	return nil, fmt.Errorf("Asset %s not found", name)
}

// AssetNames returns the names of the assets.
func AssetNames() []string {
	names := make([]string, 0, len(_bindata))
	for name := range _bindata {
		names = append(names, name)
	}
	return names
}

// _bindata is a table, holding each asset generator, mapped to its name.
var _bindata = map[string]func() ([]byte, error){
	"callgraph.avsc": callgraph_avsc,
}

// AssetDir returns the file names below a certain
// directory embedded in the file by go-bindata.
// For example if you run go-bindata on data/... and data contains the
// following hierarchy:
//     data/
//       foo.txt
//       img/
//         a.png
//         b.png
// then AssetDir("data") would return []string{"foo.txt", "img"}
// AssetDir("data/img") would return []string{"a.png", "b.png"}
// AssetDir("foo.txt") and AssetDir("notexist") would return an error
// AssetDir("") will return []string{"data"}.
func AssetDir(name string) ([]string, error) {
	node := _bintree
	if len(name) != 0 {
		cannonicalName := strings.Replace(name, "\\", "/", -1)
		pathList := strings.Split(cannonicalName, "/")
		for _, p := range pathList {
			node = node.Children[p]
			if node == nil {
				return nil, fmt.Errorf("Asset %s not found", name)
			}
		}
	}
	if node.Func != nil {
		return nil, fmt.Errorf("Asset %s not found", name)
	}
	rv := make([]string, 0, len(node.Children))
	for name := range node.Children {
		rv = append(rv, name)
	}
	return rv, nil
}

type _bintree_t struct {
	Func     func() ([]byte, error)
	Children map[string]*_bintree_t
}

var _bintree = &_bintree_t{nil, map[string]*_bintree_t{
	"callgraph.avsc": &_bintree_t{callgraph_avsc, map[string]*_bintree_t{}},
}}
