package boonleng94.iguide

import android.util.Log

import com.lemmingapex.trilateration.LinearLeastSquaresSolver
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer

import java.io.Serializable
import java.util.*

import kotlin.collections.ArrayList

data class Coordinate(var x: Double, var y: Double) : Serializable //Every 0.5m = 1 Unit

class Navigator {
    private val debugTAG = "Navigator"

    fun findNextNearest(source: Coordinate, destList: ArrayList<DestinationBeacon>): DestinationBeacon {
        //Not useful beacons
        destList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        val tree = KDTree(destList.size)

        for (i in destList) {
            Log.d(debugTAG, "destList Point: " + i.coordinate.x + ", " + i.coordinate.y)
            val point = DoubleArray(2)
            point[0] = i.coordinate.x
            point[1] = i.coordinate.y

            tree.add(point)
        }

        var start = DoubleArray(2)
        start[0] = source.x
        start[1] = source.y

        var nextPoint = tree.find_nearest(start)
        var nextCoord = Coordinate(nextPoint.x[0], nextPoint.x[1])

        Log.d(debugTAG, "Start: " + start[0] + ", " + start[1])
        Log.d(debugTAG, "nextNearest: $nextCoord")

        for (i in destList) {
            if (nextCoord == i.coordinate) {
                return i
            }
        }

        return DestinationBeacon("Beacon", 0)
    }
    //1m = 2 coordinate units = 3 steps
    fun findShortestPath(source: Coordinate, dest: Coordinate, destList: ArrayList<DestinationBeacon>, queue: Queue<DestinationBeacon>): Queue<DestinationBeacon> {
        //Not useful beacons
        destList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        val tree = KDTree(destList.size)

        for (i in destList) {
            Log.d(debugTAG, "destList Point: " + i.coordinate.x + ", " + i.coordinate.y)
            val point = DoubleArray(2)
            point[0] = i.coordinate.x
            point[1] = i.coordinate.y

            tree.add(point)
        }

        var start = DoubleArray(2)
        start[0] = source.x
        start[1] = source.y

        var nextPoint = tree.find_nearest(start)
        var nextCoord = Coordinate(nextPoint.x[0], nextPoint.x[1])

        Log.d(debugTAG, "Start: " + start[0] + ", " + start[1])
        Log.d(debugTAG, "nextNearest: $nextCoord")

        val iter = destList.iterator()

        while (iter.hasNext()) {
            val i = iter.next()

            if (i.coordinate == nextCoord) {
                queue.add(i)
                iter.remove()
            }
        }

        if (nextCoord != dest) {
            findShortestPath(nextCoord, dest, destList, queue)
        }

        return queue
    }

    fun findUserPos(destList: ArrayList<DestinationBeacon>): Coordinate {
        //Not useful beacons
        destList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }
        //Definite more than 0.5 in real life. Means inaccurately approximated
        destList.removeIf {
            item ->
            item.distance < 0.5
        }

        var size = destList.size

        var posList = Array(size) {_ -> doubleArrayOf(0.0, 0.0)}
        var distanceList = DoubleArray(size)

        //for testing
//        double[][] positions = new double[][]{{1.0, 1.0}, {1.0, 3.0}, {8.0, 8.0}, {2.0, 2.0}};
//        double[] distances = new double[]{5.0, 5.0, 6.36, 3.9};
//        double[] expectedPosition = new double[]{5.9, 2.0};
//        var testList = ArrayList<DestinationBeacon>()
//        var db1 = DestinationBeacon("Db1", 1)
//        db1.coordinate = Coordinate(1.0, 1.0)
//        db1.distance = 5.0
//        var db2 = DestinationBeacon("Db1", 1)
//        db2.coordinate = Coordinate(1.0, 3.0)
//        db2.distance = 5.0
//        var db3 = DestinationBeacon("Db1", 1)
//        db3.coordinate = Coordinate(8.0, 8.0)
//        db3.distance = 6.36
//        var db4 = DestinationBeacon("Db1", 1)
//        db4.coordinate = Coordinate(2.0, 2.0)
//        db4.distance = 3.9
//        testList.add(db1)
//        testList.add(db2)
//        testList.add(db3)
//        testList.add(db4)

        if (destList.isNotEmpty()) {
            destList.sortBy {
                it.distance
            }

            for (i in destList) {
                size--
                var pos = doubleArrayOf((i.coordinate.x)*3, (i.coordinate.y)*3)

                distanceList[size] = i.distance
                posList[size] = pos

                if (size == 0) {
                    break
                }
            }
        }

        distanceList.forEach {
            it ->
            Log.d(debugTAG, "distanceList: " + it.toString())
        }

        val trilaterationFunction = TrilaterationFunction(posList, distanceList)
        val lSolver = LinearLeastSquaresSolver(trilaterationFunction)
        val nlSolver = NonLinearLeastSquaresSolver(trilaterationFunction, LevenbergMarquardtOptimizer())

        val linearCalculatedPosition = lSolver.solve()
        val nonLinearOptimum = nlSolver.solve()

        val res1 = linearCalculatedPosition.toArray()
        val res2 = nonLinearOptimum.point.toArray()

        Log.d(debugTAG, "linear calculatedPosition: " + res1[0] + ", " + res1[1])
        Log.d(debugTAG, "non-linear calculatedPosition: "  + res2[0] + ", " + res2[1])

        return Coordinate(roundToHalf(res2[0]/3), roundToHalf(res2[1]/3))
    }

    fun getDirectionToTurn(originalOrientation: Orientation, nextOrientation: Orientation): Direction? {
        when (originalOrientation) {
            Orientation.NORTH -> when (nextOrientation) {
                Orientation.SOUTH -> return Direction.BACK
                Orientation.WEST -> return Direction.LEFT
                Orientation.EAST -> return Direction.RIGHT
            }
            Orientation.SOUTH -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.BACK
                Orientation.WEST -> return Direction.RIGHT
                Orientation.EAST -> return Direction.LEFT
            }
            Orientation.EAST -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.LEFT
                Orientation.SOUTH -> return Direction.RIGHT
                Orientation.WEST -> return Direction.BACK
            }
            Orientation.WEST -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.RIGHT
                Orientation. SOUTH -> return Direction.LEFT
                Orientation.EAST -> return Direction.BACK
            }
        }
        return null
    }

    fun getNextOrientation(userCurrentCoordinate: Coordinate?, userCurrentOrientation: Orientation, targetCoordinate: Coordinate): Orientation {
        return if (userCurrentCoordinate!!.x - targetCoordinate.x > 0) {
            Orientation.WEST
        } else if (targetCoordinate.x - userCurrentCoordinate.x > 0) {
            Orientation.EAST
        } else {
            if (userCurrentCoordinate.y - targetCoordinate.y > 0) {
                Orientation.SOUTH
            } else if (targetCoordinate.y - userCurrentCoordinate.y > 0) {
                Orientation.NORTH
            } else {
                userCurrentOrientation
            }
        }
    }

    fun computeDistance(rssi: Int, measuredPower: Int): Double {
        if (rssi == 0) {
            return -1.0
        }

        val ratio = rssi.toDouble() / measuredPower.toDouble()
        val rssiCorrection = 0.96 + Math.pow(Math.abs(rssi).toDouble(), 3.0) % 10 / 150.0

        return if (ratio <= 1.0) {
            Math.pow(ratio, 9.98) * rssiCorrection
        } else {
            (0.103 + 0.89978 * Math.pow(ratio, 7.71)) * rssiCorrection
        }
    }

    private fun roundToHalf(d: Double): Double {
        return Math.round(d * 2) / 2.0
    }
}
