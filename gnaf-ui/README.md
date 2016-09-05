# gnaf-ui

## Introduction
This project consists of static files providing a demonstration web user interface using Elasticsearch and the gnaf-service.
It uses ECMAScript 6 and so only runs in some modern browsers (Chrome, Firefox, Edge, not yet Safari).

## Configuration

The function `initBaseUrl` in `index.js` determines the URLs used to access the servers depending on the protocol used to serve the webapp.
If the `file:` protocol is used (`index.html` was opened as a file rather than from a web server) then then `http://localhost is used to access the servers.
Otherwise the protocol and host used to serve the webapp is used.

## Running and Usage

Cors access to servers isn't working from a `file:` URL.

To use python's simple web server to serve the UI over HTTP, run from the html directory: `python3 -m http.server`. Access the UI at: http://localhost:8000/.
