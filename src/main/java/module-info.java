module com.catan {
    requires javafx.controls;
    requires kotlin.stdlib;
    exports com.catan;
    // Export model packages for tests and external modules that may use them
    exports com.catan.model.player;
    exports com.catan.model.game;
    exports com.catan.model.state;
}