# staircase

[![Build Status][travis-badge]][ci]

The webapp that serves the step-based data-flow interface to the
[InterMine](http://www.intermine.org) system.

## Quick Start

Assuming leingingen, postgres and nodejs are installed:

```sh
export PORT=3000 \
       DB_USER=YOU \
       SECRET_KEY_PHRASE="some long and unguessable phrase" \
       WEB_AUDIENCE=http://localhost:$PORT
createdb staircase
lein do js-deps, run
```

Or deploy to heroku (will require provisioning a postgres DB and
setting the environment variables as below):

```sh
heroku config set BUILDPACK_URL=https://github.com/ddollar/heroku-buildpack-multi.git \
                  DB_PASSWORD=$YOUR_PASSWD \
                  DB_USER=$YOUR_USER \
                  DB_SUBNAME=//$HOST/$DBNAME \
                  DB_PORT=$DB_PORT \
                  WEB_AUDIENCE=$URL \
                  WEB_DEFAULT_SERVICE=flymine \
                  SECRET_KEY_PHRASE="long key phrase" \
                  WEB_MAX_AGE=300
git push heroku master
```

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed. This handles all
dependencies, including runtime javascript, which are managed by
[Bower][lein]. As clojure is a JVM language, this requires a JDK be installed;
please see you friendly java vendor for details.

This application also makes use of a postgres database. So you will need
[postgres][psql] installed and configured.

## Configuration

You must configure the database and add a secrets file before you can run
the application.

The secrets file is needed to sign authentication tokens. You can use
private/public key pairs, or simply provide a key phrase for HMAC hashing. In
the latter case, add a `resources/secrets.edn` file which defines a key-phrase:

```clj
{
  :key-phrase "some utterly random and unguessable key phrase"
}
```

The database and the toolset are configured using [Environ][environ], which has
a flexible configuration system. Most settings are to be found in
`profiles.clj`.

It is recommended that DB authentication settings do not live in source
controlled files, but should instead be configured through environment
variables or in the user config (`~./lein/profile.clj`).

The following configuration values are recognized, here presented
as you might configure them in a `profiles.clj` file:

```clojure
{
    :user {
        :env {
            :db-user "DB-USER"     ;; The db user (optional)
            :db-password "DB-PASS" ;; The db-password (optional)
            :db-subname "//host/dbname" ;; The db host and name
                                        ;; see profiles.clj for
                                        ;; default values.
        }
    }
}
```

These values can be configured through environment variables (see
[Environ][environ] for details), eg:

```bash
DB_USER=$PSQL_USER
```

## Running

To start a web server for the application, run:

    lein ring server

To run the tests, you will need a test database (by default named
`staircase-test`) and be able to access it. It is recommended you
add configuration values to `~/.lein/profiles.clj` for these
properties, but this can also be configured in environment
variables - see [Configuration][].

    lein test

## License

Copyright © 2014 Alex Kalderimis and InterMine

[travis-badge]: https://travis-ci.org/alexkalderimis/staircase.svg?branch=master
[ci]: https://travis-ci.org/alexkalderimis/staircase
[environ]: https://github.com/weavejester/environ
[psql]: http://www.postgresql.org/
[lein]: https://github.com/technomancy/leiningen
[bower]: http://bower.io/
