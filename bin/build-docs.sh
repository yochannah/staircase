#!/bin/sh
VERSION=$(lein get-in-project :version)

lein doc

rm -rf /tmp/staircase-docs
cp -r doc /tmp/staircase-docs

git checkout gh-pages

rm -rf ./$VERSION
rm -rf ./latest
cp -r /tmp/staircase-docs/ ./latest
cp -r /tmp/staircase-docs/ ./$VERSION

git add --all ./$VERSION
git add --all ./latest
git commit -a -m "Update ${VERSION} doc"
