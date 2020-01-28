package strongdmm.byond.dmm

import io.github.spair.dmm.io.DmmData
import io.github.spair.dmm.io.TileContent
import strongdmm.byond.dme.Dme
import strongdmm.util.inline.RelPath
import java.io.File
import java.nio.file.Paths

class Dmm(
    mapFile: File,
    initialDmmData: DmmData,
    dme: Dme
) {
    companion object {
        const val MAP_ID_NONE: Int = -1
    }

    val mapName: String = mapFile.nameWithoutExtension
    val absMapPath: String = mapFile.absolutePath
    val relMapPath: RelPath = RelPath(Paths.get(dme.rootPath).relativize(mapFile.toPath()).toString())
    val id: Int = absMapPath.hashCode()

    var maxX: Int = initialDmmData.maxX
        private set
    var maxY: Int = initialDmmData.maxY
        private set

    private var tiles: Array<Array<LongArray>> = Array(maxY) { Array(maxX) { LongArray(0) } }

    init {
        for (x in 1..maxX) {
            for (y in 1..maxY) {
                var tileItems = LongArray(0)

                initialDmmData.getTileContentByLocation(x, y)?.let {
                    for (tileObject in it) {
                        if (dme.getItem(tileObject.type) != null) {
                            tileItems += GlobalTileItemHolder.getOrCreate(tileObject.type, tileObject.vars).id
                        }
                    }
                }

                tiles[y - 1][x - 1] = tileItems
            }
        }
    }

    fun getTileContentByLocation(x: Int, y: Int): TileContent {
        return getTile(x, y).getTileContent()
    }

    fun getTile(x: Int, y: Int): Tile = Tile(this, x, y)
    fun getTileItemsId(x: Int, y: Int): LongArray = tiles[y - 1][x - 1]

    fun setTileItemsId(x: Int, y: Int, tileItemsId: LongArray) {
        tiles[y - 1][x - 1] = tileItemsId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Dmm
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id
    }
}
