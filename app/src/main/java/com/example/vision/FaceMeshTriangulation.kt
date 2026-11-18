package com.example.vision

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

object FaceMeshTriangulation {

    val innerLipRing: IntArray = intArrayOf(
        78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
        308, 324, 318, 402, 317, 14, 87, 178, 88, 95,
    )

    val faceOvalSet: Set<Int> = setOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
        172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
    )

    private val innerLipSet: Set<Int> = innerLipRing.toSet()

    val triangleIndices: ShortArray by lazy {
        val adj = HashMap<Int, MutableSet<Int>>(512)
        for (edge in FaceLandmarker.FACE_LANDMARKS_TESSELATION) {
            val u = edge.start()
            val v = edge.end()
            if (u == v) continue
            adj.getOrPut(u) { HashSet() }.add(v)
            adj.getOrPut(v) { HashSet() }.add(u)
        }

        val indices = ArrayList<Short>(2700)
        for ((u, uNeighbors) in adj) {
            for (v in uNeighbors) {
                if (v <= u) continue
                val vNeighbors = adj[v] ?: continue
                for (w in vNeighbors) {
                    if (w <= v) continue
                    if (!uNeighbors.contains(w)) continue
                    if (innerLipSet.contains(u) && innerLipSet.contains(v) && innerLipSet.contains(w)) {
                        continue
                    }
                    indices.add(u.toShort())
                    indices.add(v.toShort())
                    indices.add(w.toShort())
                }
            }
        }
        indices.toShortArray()
    }
}
