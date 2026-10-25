@echo off
setlocal
if not exist db.properties copy db.properties.example db.properties
java --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp "sige-1.0.0.jar;lib/*" com.unev.sige.app.App