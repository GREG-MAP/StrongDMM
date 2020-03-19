package strongdmm.event.type.controller

import strongdmm.byond.dmm.MapPos
import strongdmm.byond.dmm.TileItem
import strongdmm.event.Event
import strongdmm.event.TileItemType

abstract class EventMapModifierController {
    class ReplaceTypeInPositions(body: Pair<TileItemType, List<Pair<TileItem, MapPos>>>) :
        Event<Pair<TileItemType, List<Pair<TileItem, MapPos>>>, Unit>(body, null)

    class ReplaceIdInPositions(body: Pair<TileItemType, List<Pair<TileItem, MapPos>>>) :
        Event<Pair<TileItemType, List<Pair<TileItem, MapPos>>>, Unit>(body, null)

    class DeleteTypeInPositions(body: List<Pair<TileItem, MapPos>>) : Event<List<Pair<TileItem, MapPos>>, Unit>(body, null)
    class DeleteIdInPositions(body: List<Pair<TileItem, MapPos>>) : Event<List<Pair<TileItem, MapPos>>, Unit>(body, null)
}
