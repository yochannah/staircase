#!/bin/sh
VERSION=$(lein get-in-project :version)

lein doc

rm -rf /tmp/staircase-docs
mv doc /tmp/staircase-docs

git checkout gh-pages

rm -rf ./$VERSION
rm -rf ./latest
mv /tmp/staircase-docs/ ./latest

git add --all ./$VERSION
git add --all ./latest
git commit -a -m "Update ${VERSION} doc"
