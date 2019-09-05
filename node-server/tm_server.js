/* jshint esversion:6 */
const express = require('express');
var app = express();
const APP_PORT = 8877;

app.use(express.json());

app.all('/translate', function (req, res) {
  console.log('%s %s', req.method, req.url);

  var textToTranslate = req.query.text;

  // Do something more useful here...
  textToTranslate = req.query.source + ">" + req.query.target + " => [" + req.query.text.toUpperCase() + "]";

  res.status(200).send({ translation: textToTranslate});

});

app.listen(APP_PORT, function () {
  console.log('Fake TM Server listening on port %i!', APP_PORT);
});
