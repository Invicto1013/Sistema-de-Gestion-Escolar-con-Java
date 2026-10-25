#!/usr/bin/env sh
set -eu
if [ ! -f db.properties ]; then cp db.properties.example db.properties; fi
exec java --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp "sige-1.0.0.jar:lib/*" com.unev.sige.app.App