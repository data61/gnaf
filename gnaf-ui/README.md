# gnaf-ui

## Introduction
This project consists of static files providing a demonstration web user interface using Elasticsearch and the gnaf-service.
It uses ECMAScript 6 and so only runs in some modern browsers (Chrome, Firefox, Edge, not yet Safari).

The function `initBaseUrl` in `index.js` determines the URLs used to access the servers depending on the protocol used to serve the webapp.
If the `file:` protocol is used (`index.html` was opened as a file rather than from a web server) then then `http://localhost:9200` is used for Elasticsearch and `http://localhost:9000` is used for the gnaf database service.
Otherwise the protocol and host used to serve the webapp is used (the ports stay the same).