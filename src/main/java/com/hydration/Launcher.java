package com.hydration;

import javafx.application.Application;

/**
 * Punto de entrada real para ejecutar desde un fat jar / ejecutable
 * single-file.
 *
 * JavaFX 11+ se niega a arrancar (LauncherHelper: "JavaFX runtime components
 * are missing") cuando el main class extiende {@code Application} y no se usa
 * el module path. Delegar en {@link Application#launch} desde una clase que
 * no extienda {@code Application} evita ese chequeo y permite correr desde
 * el classpath (fat jar, Launch4j, etc.).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        HydrationApp.main(args);
    }
}
