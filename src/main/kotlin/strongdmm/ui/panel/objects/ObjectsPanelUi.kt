package strongdmm.ui.panel.objects

import strongdmm.Processable

class ObjectsPanelUi : Processable {
    private val state = State()
    private val view = View(state)
    private val viewController = ViewController(state)
    private val eventController = EventController(state)

    init {
        view.viewController = viewController
        eventController.viewController = viewController
    }

    override fun process() {
        view.process()
    }
}
