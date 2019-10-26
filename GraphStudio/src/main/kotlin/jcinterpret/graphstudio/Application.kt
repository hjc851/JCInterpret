package jcinterpret.graphstudio

import javafx.scene.Scene
import javafx.stage.Stage
import jcinterpret.graphstudio.scene.GraphStudioScene
import jcinterpret.graphstudio.util.FXMLResource
import com.apple.eawt.Application as NSApplication
import javafx.application.Application as FXApplication

class Application: FXApplication() {
    override fun start(primaryStage: Stage) {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

        val loader = FXMLResource.forClass(GraphStudioScene::class)

        primaryStage.title = "Graph Studio"
        primaryStage.scene = Scene(loader.load())
        primaryStage.show()
    }
}