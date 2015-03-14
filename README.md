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
heroku config:set DB_PASSWORD=$YOUR_PASSWD \
                  DB_USER=$YOUR_USER \
                  DB_SUBNAME=//$HOST/$DBNAME \
                  DB_PORT=$DB_PORT \
                  WEB_AUDIENCE=$URL \ 
                  WEB_DEFAULT_SERVICE=flymine \
                  SECRET_KEY_PHRASE="long key phrase" \
                  WEB_MAX_AGE=300
git push heroku master
```

The key-phrase can be any string, but it should be kept secure. The audience
must be the same as the public URL of the site, or else Persona login will not
work. Additional setting can be provided as follows:

```sh
# activate google analytics
heroku config:set CLIENT_GA_TOKEN=$YOUR_TOKEN
# Enable the clojure repl for debugging access
heroku config:set WEB_REPL_USER=$YOU WEB_REPL_PWD=$YOUR_PWD
```

We build with the buildpack-multi buildpack, since we have a two-stage
nodejs/clojure build. This is set in the `.env` file.

## Prerequisites and Dependencies

You will need [Leiningen][1] 2.0 or above installed (2.4+ to use the web-repl). This handles all
dependencies. As clojure is a JVM language, this requires a JDK (1.6+) be installed;
please see your friendly java vendor for details.

A [node-js][nodejs] environment is also required, which handles the
installation of the javascript dependencies using [npm][npm] and
[Bower][bower].

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
variables or in the user config (`~./lein/profile.clj`). See the heroku section
above for details on the use of environment variables for this purpose.

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
