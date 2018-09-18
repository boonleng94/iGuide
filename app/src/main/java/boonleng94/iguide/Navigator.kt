package boonleng94.iguide

import android.util.Log

import com.lemmingapex.trilateration.LinearLeastSquaresSolver
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer

import java.io.Serializable
import java.util.*

import kotlin.collections.ArrayList
import kotlin.math.roundToInt

data class Coordinate(var x: Double, var y: Double) : Serializable //Every 0.5m = 1 Unit

class Navigator {
    private val debugTAG = "Navigator"

    //1m = 2 coordinate units = 3 steps
    //tried a total of 100 trilats accuracy highest if can use all beacons
    //trilat 6 - 90% accurate
    //trilat 5 - 89% accurate
    //trilat 4 - 88% accurate
    //trilat 3 - 85% accurate
    //trilat using lesser and lesser beacons, get the trilats with delta < 2 (~1m), average (can be done with weightage next time)

    fun findUserPos(destList: ArrayList<DestinationBeacon>): Coordinate {
        destList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        var deltaLimit = 2
        var size = destList.size
        var posList = Array(size) {_ -> doubleArrayOf(0.0, 0.0)}
        var distanceList = DoubleArray(size)

        if (destList.isNotEmpty()) {
            destList.sortBy {
                it.distance
            }

            for (i in destList) {
                size--
                var pos = doubleArrayOf(i.coordinate.x.toDouble(), i.coordinate.y.toDouble())

                distanceList[size] = i.distance
                posList[size] = pos

                if (size == 0) {
                    break
                }
            }
        }

        //posList/distanceList = big to small distances

        //Loop:
        //1 - count = destList.size
        //2 - count = destList.size - 1
        //3 - count = destList.size - 2
        //....
        //Last count = destList.size = 3
        var index = 0
        var res = DoubleArray(2)
        var count = destList.size

        posList.forEach {
            it ->
            Log.d(debugTAG, "posList: " + it.toString())
        }
        distanceList.forEach {
            it ->
            Log.d(debugTAG, "distanceList: " + it.toString())
        }


        while (count != 3) {
            var tempPosList = Arrays.copyOfRange(posList, index, posList.size)
            var tempDistanceList = Arrays.copyOfRange(distanceList, index, distanceList.size)

            val trilaterationFunction = TrilaterationFunction(tempPosList, tempDistanceList)
            val lSolver = LinearLeastSquaresSolver(trilaterationFunction)
            val nlSolver = NonLinearLeastSquaresSolver(trilaterationFunction, LevenbergMarquardtOptimizer())

            val linearCalculatedPosition = lSolver.solve()
            val nonLinearOptimum = nlSolver.solve()

//            val res1 = linearCalculatedPosition.toArray()
//            val res2 = nonLinearOptimum.point.toArray()
//
//            Log.d("iGuide", "linear calculatedPosition: $res1")
//            Log.d("iGuide", "non-linear calculatedPosition: $res2")

            if (res.isEmpty()) {
                res[0] = linearCalculatedPosition.toArray()[0]
                res[1] = linearCalculatedPosition.toArray()[1]
            } else {
                val delta = linearCalculatedPosition.toArray()[0] - (res[0] / res.size)
                val delta1 = linearCalculatedPosition.toArray()[1] - (res[1] / res.size)

                if (delta < deltaLimit && delta1 < deltaLimit) {
                    res[0] += linearCalculatedPosition.toArray()[0]
                    res[1] += linearCalculatedPosition.toArray()[1]
                }
            }

            count--
            index++
        }

        res[0] = res[0] / res.size
        res[1] = res[1] / res.size

        return Coordinate(res[0], res[1])
    }
}
