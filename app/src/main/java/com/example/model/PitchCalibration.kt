package com.example.model

import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

data class PitchCalibration(
    val pitchCorners: List<Offset>, // 4 points: [nearLeft, nearRight, farRight, farLeft]
    val stumpBase: Offset,          // Batsman stump base
    val wideLine: List<Offset>,     // 2 points: [wideLeft, wideRight] along batting crease
    val poppingCrease: List<Offset> = listOf(pitchCorners[0], pitchCorners[1]) // Bowler crease
) {
    fun toJson(): String {
        val root = JSONObject()
        
        val cornersArr = JSONArray()
        pitchCorners.forEach { pt ->
            cornersArr.put(JSONObject().apply {
                put("x", pt.x.toDouble())
                put("y", pt.y.toDouble())
            })
        }
        root.put("pitchCorners", cornersArr)

        val stumpObj = JSONObject().apply {
            put("x", stumpBase.x.toDouble())
            put("y", stumpBase.y.toDouble())
        }
        root.put("stumpBase", stumpObj)

        val wideArr = JSONArray()
        wideLine.forEach { pt ->
            wideArr.put(JSONObject().apply {
                put("x", pt.x.toDouble())
                put("y", pt.y.toDouble())
            })
        }
        root.put("wideLine", wideArr)

        val creaseArr = JSONArray()
        poppingCrease.forEach { pt ->
            creaseArr.put(JSONObject().apply {
                put("x", pt.x.toDouble())
                put("y", pt.y.toDouble())
            })
        }
        root.put("poppingCrease", creaseArr)

        return root.toString()
    }

    companion object {
        fun default(): PitchCalibration {
            return PitchCalibration(
                pitchCorners = listOf(
                    Offset(0.18f, 0.88f), // Near left (bowler popping crease left)
                    Offset(0.82f, 0.88f), // Near right (bowler popping crease right)
                    Offset(0.68f, 0.28f), // Far right (batsman crease right)
                    Offset(0.32f, 0.28f)  // Far left (batsman crease left)
                ),
                stumpBase = Offset(0.50f, 0.26f),
                wideLine = listOf(
                    Offset(0.36f, 0.28f), // Left wide marker
                    Offset(0.64f, 0.28f)  // Right wide marker
                ),
                poppingCrease = listOf(
                    Offset(0.18f, 0.88f),
                    Offset(0.82f, 0.88f)
                )
            )
        }

        fun fromJson(jsonStr: String): PitchCalibration {
            try {
                val root = JSONObject(jsonStr)
                val cornersArr = root.getJSONArray("pitchCorners")
                val corners = mutableListOf<Offset>()
                for (i in 0 until cornersArr.length()) {
                    val obj = cornersArr.getJSONObject(i)
                    corners.add(Offset(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat()))
                }

                val stumpObj = root.getJSONObject("stumpBase")
                val stump = Offset(stumpObj.getDouble("x").toFloat(), stumpObj.getDouble("y").toFloat())

                val wideArr = root.getJSONArray("wideLine")
                val wide = mutableListOf<Offset>()
                for (i in 0 until wideArr.length()) {
                    val obj = wideArr.getJSONObject(i)
                    wide.add(Offset(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat()))
                }

                val crease = if (root.has("poppingCrease")) {
                    val creaseArr = root.getJSONArray("poppingCrease")
                    val list = mutableListOf<Offset>()
                    for (i in 0 until creaseArr.length()) {
                        val obj = creaseArr.getJSONObject(i)
                        list.add(Offset(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat()))
                    }
                    list
                } else {
                    listOf(corners[0], corners[1])
                }

                return PitchCalibration(corners, stump, wide, crease)
            } catch (_: Exception) {
                return default()
            }
        }
    }
}
