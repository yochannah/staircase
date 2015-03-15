#!/bin/env node

'use strict';

var fs = require('fs');
var edn = require('jsedn');
var path = require('path');
var bower = require('bower');

var installConf = {
  interactive: false,
  cwd: path.join(__dirname, '..', 'external'), // The point of this is to ignore the bower.json in the project root.
  directory: 'tools' // Install tools into the external tools directory.
};

var configFile = path.join(__dirname, '..', 'resources', 'config.edn');

// TODO: consolidate tool dependencies.
fs.readFile(configFile, {encoding: 'utf8'}, function (err, data) {
  if (err) {
    console.error(err);
    process.exit(1);
  }
  var config = edn.parse(data);
  var tools = config.at(edn.kw(':bower-tools'));
  if (tools) {
    var toInstall = [];
    tools.each(function (coords) {
      toInstall.push(edn.toJS(coords.at(0)) + '=' + edn.toJS(coords.at(1)));
    });
    console.log("Installing: " + toInstall.join(', '));
    bower.commands
    .install(toInstall, {save: false}, installConf)
    .on('log', function (data) {
      switch (data.level) {
        case 'action':
        case 'error':
        case 'warn':
          console.log('[bower ' + data.level + '] ', data.id, data.message);
          break;
      }
    })
    .on('end', function (installed) {
      for (var key in installed) {
        var details = installed[key];
        var ep = details.endpoint;
        console.log('[bower] Installed ' + ep.name + ' ' + ep.target + ' from ' + ep.source);
        console.log('[bower] --> to ' + details.canonicalDir);
      }
      process.exit(0);
    });
  } else {
      process.exit(0);
  }
});

