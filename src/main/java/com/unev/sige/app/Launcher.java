package com.unev.sige.app;

/**
 * Punto de entrada para el JAR ejecutable y para jpackage.
 *
 * Una clase que extiende javafx.application.Application no puede ser la clase principal
 * de un JAR "fat" que lleva JavaFX en el classpath (Java exige JavaFX como modulo y falla
 * con "faltan componentes de JavaFX runtime"). Esta clase intermedia no extiende Application,
 * asi que evita ese problema y solo delega en App.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
