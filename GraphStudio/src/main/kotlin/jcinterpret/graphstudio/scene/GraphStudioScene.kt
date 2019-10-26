package jcinterpret.graphstudio.scene

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.MenuBar
import javafx.stage.Stage

class GraphStudioScene {

    @FXML lateinit var menubar: MenuBar

    @FXML
    fun exit(e: ActionEvent) {
        val stage = menubar.scene.window as Stage
        stage.close()
    }

    @FXML
    fun open(e: ActionEvent) {

    }
}