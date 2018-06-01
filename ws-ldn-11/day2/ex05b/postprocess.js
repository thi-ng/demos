var fs = require('fs');
var path = 'resources/public/js/meshworker.js';
var include = 'self.importScripts("base.js");';
src = include + fs.readFileSync(path,'utf8').replace(include, '');
fs.writeFileSync(path, src, 'utf8');
