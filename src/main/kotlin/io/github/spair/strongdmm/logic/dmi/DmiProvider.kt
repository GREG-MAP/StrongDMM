package io.github.spair.strongdmm.logic.dmi

import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.chunks.PngMetadata
import io.github.spair.strongdmm.DI
import io.github.spair.strongdmm.logic.Environment
import org.kodein.di.erased.instance
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import javax.imageio.ImageIO

class DmiProvider {

    companion object {
        val PLACEHOLDER_IMAGE = ImageIO.read(DmiProvider::class.java.classLoader.getResource("placeholder.png"))!!

        private val WIDTH_HEIGHT_PATTERN = "(?:width\\s=\\s(\\d+))\n\t(?:height\\s=\\s(\\d+))".toRegex()
        private val STATE_PATTERN = "(?:state\\s=\\s\".*\"(?:\\n\\t.*)+)".toRegex()
        private val PARAM_PATTERN = "(\\w+)\\s=\\s(.+)".toRegex()
    }

    private val environment by DI.instance<Environment>()
    private val dmiCache = mutableMapOf<String, Dmi?>()

    fun getDmi(icon: String): Dmi? {
        if (icon.isEmpty()) {
            return null
        }

        if (dmiCache.containsKey(icon)) {
            return dmiCache[icon]
        }

        val dmiFile = File(environment.getAbsolutePath() + File.separator + icon)

        if (!dmiFile.exists() || !dmiFile.isFile) {
            dmiCache[icon] = null
            return null
        }

        val dmiImage: BufferedImage
        val imageMeta: Metadata

        with(PngReader(dmiFile)) {
            dmiImage = extractAtlasImage(this)
            imageMeta = extractMetadata(metadata)
            close()
        }

        val dmiCols = dmiImage.width / imageMeta.spriteWidth
        val dmiRows = dmiImage.height / imageMeta.spriteHeight

        val iconStates = mutableMapOf<String, IconState>()
        val dmi = Dmi(dmiImage, imageMeta.spriteWidth, imageMeta.spriteHeight, dmiRows, dmiCols, iconStates)

        var spriteIndex = 0

        imageMeta.states.forEach { state ->
            val iconSprites = mutableListOf<IconSprite>()

            for (i in 0 until state.dirs * state.frames) {
                iconSprites.add(IconSprite(dmi, spriteIndex++))
            }

            with(state) {
                iconStates[name] = IconState(name, dirs, frames, iconSprites)
            }
        }

        dmiCache[icon] = dmi
        return dmi
    }

    private fun extractAtlasImage(pngReader: PngReader) = with(pngReader) {
        val atlasImage = BufferedImage(imgInfo.cols, imgInfo.rows, BufferedImage.TYPE_INT_ARGB)

        val buffer = atlasImage.raster.dataBuffer as DataBufferInt
        val pal = metadata.plte
        val trns = metadata.trns

        val data = buffer.data
        var dataindex = 0

        when {
            imgInfo.indexed -> {
                val tlen = trns?.palletteAlpha?.size ?: 0

                while (hasMoreRows()) {
                    val line = readRow() as ImageLineInt
                    val lineArray = line.scanline

                    for (index in lineArray) {
                        if (index < tlen) {
                            data[dataindex++] = (trns.palletteAlpha[index] shl 24) + pal.getEntry(index)
                        } else {
                            data[dataindex++] = (pal.getEntry(index) + 0xff000000).toInt()
                        }
                    }
                }
            }
            imgInfo.greyscale -> while (hasMoreRows()) {
                val line = readRow() as ImageLineInt
                val lineArray = line.scanline
                var i = 0

                while (i < lineArray.size) {
                    val value = lineArray[i]
                    var alpha = 255

                    if (imgInfo.channels == 2) {
                        alpha = lineArray[i + 1]
                    }

                    data[dataindex++] = value + (value shl 8) + (value shl 16) + (alpha shl 24)
                    i += imgInfo.channels
                }
            }
            else -> while (hasMoreRows()) {
                val line = readRow() as ImageLineInt
                val lineArray = line.scanline
                var i = 0

                while (i < lineArray.size) {
                    val r = lineArray[i]
                    val g = lineArray[i + 1]
                    val b = lineArray[i + 2]
                    val alpha = if (imgInfo.channels == 4) lineArray[i + 3] else 255
                    data[dataindex++] = b + (g shl 8) + (r shl 16) + (alpha shl 24)
                    i += imgInfo.channels
                }
            }
        }

        atlasImage
    }

    private fun extractMetadata(imageMeta: PngMetadata): Metadata {
        val textMeta = imageMeta.getTxtForKey("Description")

        val widthHeight = WIDTH_HEIGHT_PATTERN.toPattern().matcher(textMeta).apply { find() }
        val width = widthHeight.group(1)?.toInt() ?: 32
        val height = widthHeight.group(2)?.toInt() ?: 32

        val states = mutableListOf<MetadataState>()

        with(STATE_PATTERN.toPattern().matcher(textMeta)) {
            while (find()) {
                val stateParam = PARAM_PATTERN.toPattern().matcher(group())

                var name = ""
                var dirs = 1
                var frames = 1

                while (stateParam.find()) {
                    val paramName = stateParam.group(1)
                    val paramValue = stateParam.group(2)

                    when (paramName) {
                        "state" -> name = paramValue.substring(1, paramValue.length - 1)
                        "dirs" -> dirs = paramValue.toInt()
                        "frames" -> frames = paramValue.toInt()
                    }
                }

                states.add(MetadataState(name, dirs, frames))
            }
        }

        return DmiProvider.Metadata(width, height, states)
    }

    private data class Metadata(val spriteWidth: Int, val spriteHeight: Int, val states: List<MetadataState>)
    private data class MetadataState(val name: String, val dirs: Int, val frames: Int)
}