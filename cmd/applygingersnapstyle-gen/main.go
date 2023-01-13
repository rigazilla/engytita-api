package main

import (
	"flag"
	"fmt"
	"go/ast"
	"go/format"
	"go/parser"
	"go/token"
	"os"
	"strings"

	"github.com/fatih/structtag"
	"github.com/iancoleman/strcase"
)

func main() {
	rmInfilePtr := flag.Bool("rm", false, "remove input file")
	flag.Parse()
	args := flag.Args()
	fset := token.NewFileSet()

	if len(args) < 2 {
		fmt.Fprintf(os.Stderr, "error: missing arguments, infile outfile needed\n")
		os.Exit(1)
	}
	fileName := args[0]
	{
		outFile, err := os.Create(args[1])
		if err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			os.Exit(1)
		}
		defer outFile.Close()
		// get ast Node of whole file;
		ff, err := parser.ParseFile(fset, fileName, nil, parser.ParseComments)
		if err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			os.Exit(1)
		}
		ast.Walk(visitor{}, ff)
		if err := format.Node(outFile, fset, ff); err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			os.Exit(1)
		}
	}
	if *rmInfilePtr {
		os.Remove(fileName)
	}
}

type visitor struct{}

func (v visitor) Visit(n ast.Node) ast.Visitor {
	if n == nil {
		return nil
	}
	switch field := n.(type) {
	case *ast.Field:
		if tag := field.Tag; tag != nil {
			tags, _ := structtag.Parse(strings.Trim(field.Tag.Value, "`"))
			for i := range tags.Tags() {
				if tags.Tags()[i].Key == "json" {
					tags.Tags()[i].Name = strcase.ToLowerCamel(tags.Tags()[i].Name)
				}
				field.Tag.Value = "`" + tags.String() + "`"
			}
		}
	}
	return v
}
