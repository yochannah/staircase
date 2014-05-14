#!/usr/bin/bash

set -e

BOWER=$(which bower)
NPM=$(which npm)

if test -z "$BOWER"; then
	echo "Using local bower"
	$NPM install bower
	./node_modules/bower/bin/bower install 
else
	echo "Using system bower"
	$BOWER install
fi

echo "Installed client dependencies"
