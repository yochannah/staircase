#!/usr/bin/bash

set -e

BOWER=$(which bower)
NPM=$(which npm)
LEIN=$(which lein)

if test -z "$BOWER"; then
	$NPM install bower
	./node_modules/bower/bin/bower install 
else
	$BOWER install
fi

$LEIN with-profile production trampoline ring server-headless
