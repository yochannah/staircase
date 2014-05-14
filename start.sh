#!/usr/bin/bash

BOWER=$(which bower)
NPM=$(which npm)


if test -z "$BOWER"; then
	if test -z "$NPM"; then 
		echo "npm not found. Cannot install bower" >> error.log
		exit 1
	fi
	echo "Using local bower"
	$NPM install bower
	./node_modules/bower/bin/bower install 
else
	echo "Using system bower"
	$BOWER install
fi

echo "Installed client dependencies"
