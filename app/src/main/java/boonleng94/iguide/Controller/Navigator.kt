package boonleng94.iguide.Controller

import android.util.Log
import boonleng94.iguide.Model.Beacon
import boonleng94.iguide.Model.Direction
import boonleng94.iguide.Model.KDTree
import boonleng94.iguide.Model.Orientation

import com.lemmingapex.trilateration.LinearLeastSquaresSolver
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer

import java.io.Serializable
import java.util.*

import kotlin.math.roundToInt

//Data class to store Coordinates
data class Coordinate(var x: Double, var y: Double) : Serializable
//2 units = ~1m = 3 steps

class Navigator {
    private lateinit var currentPos: Coordinate
    private lateinit var destination: Coordinate
    private lateinit var currentOrientation: Orientation
    private var dbList: ArrayList<Beacon> = ArrayList()
    private lateinit var userPos: Coordinate
    private lateinit var userOrientation: Orientation
    private lateinit var travelCost: Array<IntArray>
    private lateinit var neighbourCoordinates: Array<Coordinate?>

    private var maxX: Double = -1.0
    private var maxY: Double = -1.0
    private var parentCoordinates = HashMap<Coordinate, Coordinate>()
    private var coordsToVisit = ArrayList<Coordinate>()
    private var coordsVisited = ArrayList<Coordinate>()

    companion object {
        private val debugTAG = "Navigator"
        private const val MOVEMENT_COST = 1
        private const val TURNING_COST = 3
        private const val MAX_COST = 99999
    }

    fun initialize(currentPos: Coordinate, currentOrientation: Orientation, destination: Coordinate, destList: ArrayList<Beacon>) {
        this.currentPos = currentPos
        this.destination = destination
        this.currentOrientation = currentOrientation
        this.userPos = Coordinate(currentPos.x, currentPos.y)
        this.userOrientation = currentOrientation

        for (p in destList) {
            dbList.add(p.clone())
        }

        dbList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        this.maxX = dbList[0].coordinate.x
        this.maxY = dbList[0].coordinate.y

        neighbourCoordinates = Array(4, { null })

        for (i in dbList) {
            Log.d(debugTAG, "Beacon info: " + i.name + " coord: " + i.coordinate)

            if (i.coordinate.x > maxX) {
                maxX = i.coordinate.x
            }
            if (i.coordinate.y > maxY) {
                maxY = i.coordinate.y
            }
        }

        maxX+=1
        maxY+=1

        travelCost = Array(maxX.toInt(), { IntArray(maxY.toInt()) })

        for (i in 0 until (maxX).roundToInt()) {
            for (j in 0 until (maxY).roundToInt()) {
                travelCost[i][j] = MAX_COST
            }
        }

        for (i in 0 until (maxX).roundToInt()) {
            for (j in 0 until (maxY).roundToInt()) {
                for (d in dbList) {
                    if (i == d.coordinate.x.toInt()) {
                        travelCost[i][j] = 0
                    }

                    if (j == d.coordinate.y.toInt()) {
                        travelCost[i][j] = 0
                    }
                }
            }
        }

        coordsToVisit.add(currentPos)
        travelCost[currentPos.x.roundToInt()][currentPos.y.roundToInt()] = 0
    }

    /**
     * Returns true if the given coordinates are within the Map boundaries
     */
    private fun checkValidCoordinates(y: Double, x: Double): Boolean {
        return y >= 0 && x >= 0 && y < maxY && x < maxX
    }

    /**
     * Returns the first optimal coordinate that can be traversed to reach the destination (lowest cost - to ensure shortest path)
     */
    private fun optimalCoordinate(destination: Coordinate): Coordinate? {
        val size = coordsToVisit.size
        var minCost = 99.00
        var result: Coordinate? = null

        // pathCost is the cost of the path from a Coordinate to destination
        for (i in size - 1 downTo 0) {
            val pathCost = travelCost[coordsToVisit[i].x.roundToInt()][coordsToVisit[i].y.roundToInt()]
            val cost = pathCost + heuristicCost(coordsToVisit[i], destination)
            if (cost < minCost) {
                minCost = cost
                result = coordsToVisit[i]
            }
        }
        return result
    }

    /**
     * Returns the Orientation that the user need to turn to move from current Coordinate to the target Coordinate
     */
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

    /**
     * Returns the moving/travel cost required for the user to move from origin Coordinate to destination Coordinate(assuming the coordinates are neighbours of each other).
     */
    private fun getMoveCost(originCoordinate: Coordinate, destinationCoordinate: Coordinate, orientation: Orientation): Double {
        val moveCost = MOVEMENT_COST.toDouble()

        val targetDir = getNextOrientation(originCoordinate, orientation, destinationCoordinate)

        var numOfTurn = Math.abs(orientation.ordinal - targetDir.ordinal)
        if (numOfTurn > 2) {
            numOfTurn %= 2
        }
        val turnCost = (numOfTurn * TURNING_COST).toDouble()

        return moveCost + turnCost
    }

    /**
     * Returns the heuristic cost needed to traverse from a given Coordinate to the destination
     * Heuristics cost - estimates the cost of the cheapest path from Coordinate to the destination. (time taken)
     * Wiki - For the algorithm to find the actual shortest path, the heuristic function must be admissible,
     * meaning that it never overestimates the actual cost to get to the nearest destination node.
     */
    private fun heuristicCost(coordinate: Coordinate, destination: Coordinate): Double {
        // The actual number of moves required (difference in the rows and columns)
        val movementCost = (Math.abs(destination.x - coordinate.x) + Math.abs(destination.y - coordinate.y)) * MOVEMENT_COST

        if (movementCost.roundToInt() == 0) {
            return 0.0
        }

        // If destination is not in the same row or column, one orientation turn will be required
        var turnCost = 0.0
        if ((destination.x - coordinate.x).roundToInt() !== 0 || (destination.y - coordinate.y).roundToInt() !== 0) {
            turnCost = TURNING_COST.toDouble()
        }

        return movementCost + turnCost
    }

    /**
     * Returns the Direction that the user needs to turn to face the next Orientation from the original Orientation
     */
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

    /**
     * Returns the Queue of beacons that the user should pass to reach the destination the fastest
     * This method is for main program to call
     */
    fun executeFastestPath(): Queue<Beacon> {
        Log.i(debugTAG, "Fastest path from (" + currentPos.x + ", " + currentPos.y + ") to (" + destination.x + ", " + destination.y + ")...")
        // Stack to contain the shortest path/mandatory coordinates the user should take (for backtracking later)
        val shortestPathStack = Stack<Coordinate>()

        do {
            // Get the Coordinate with the smallest cost from the array of Coordinates to visit and assign it as the current Coordinate
            currentPos = optimalCoordinate(destination)!!

            // Change the Orientation of the user to face the current Coordinate from the previous Coordinate
            if (parentCoordinates.containsKey(currentPos)) {
                currentOrientation = getNextOrientation(parentCoordinates[currentPos], currentOrientation, currentPos)
            }

            // Add current Coordinate to array of visited Coordinates and remove from array of Coordinates to visit
            coordsVisited.add(currentPos)
            coordsToVisit.remove(currentPos)

            // If the destination coordinate is in the array of visited Coordinates - a path to the destination coordinate has been found, find the fastest path
            if (coordsVisited.contains(destination)) {
                var temp: Coordinate? = destination

                while (true) {
                    shortestPathStack.push(temp)

                    temp = parentCoordinates[temp]

                    if (temp == null) {
                        break
                    }
                }
                return calcFastestPath(shortestPathStack, destination)
            }

            // Store the neighbour coordinates of the current Coordinate into the array (Up, down, left, right) - null if the respective neighbour coordinate is an obstacle
            // Up neighbour coordinate
            if (checkValidCoordinates(currentPos.y + 1, currentPos.x)) {
                neighbourCoordinates[0] = Coordinate(currentPos.x, currentPos.y + 1)
            }
            // Down neighbour coordinate
            if (checkValidCoordinates(currentPos.y - 1, currentPos.x)) {
                neighbourCoordinates[1] = Coordinate(currentPos.x, currentPos.y - 1)
            }
            // Left neighbour coordinate
            if (checkValidCoordinates(currentPos.y, currentPos.x - 1)) {
                neighbourCoordinates[2] = Coordinate(currentPos.x - 1, currentPos.y)
            }
            // Right neighbour coordinate
            if (checkValidCoordinates(currentPos.y, currentPos.x + 1)) {
                neighbourCoordinates[3] = Coordinate(currentPos.x + 1, currentPos.y)
            }

            // For-loop to iterate through the neighbour Coordinates and update the travel cost of each Coordinate
            for (i in neighbourCoordinates) {
                if (i != null) {
                    // The neighbour coordinate has already been visited previously
                    if (coordsVisited.contains(i)) {
                        continue
                    }

                    if (!coordsToVisit.contains(i)) {
                        parentCoordinates.put(i, currentPos)
                        travelCost[i.x.toInt()][i.y.toInt()] = travelCost[currentPos.x.toInt()][currentPos.y.toInt()] + getMoveCost(currentPos, i, currentOrientation).toInt()
                        coordsToVisit.add(i)
                    } else {
                        val currentCost = travelCost[i.x.toInt()][i.y.toInt()]
                        val updatedCost = travelCost[currentPos.x.toInt()][currentPos.y.toInt()] + getMoveCost(currentPos, i, currentOrientation)
                        if (updatedCost < currentCost) {
                            travelCost[i.x.toInt()][i.y.toInt()] = updatedCost.toInt()
                            parentCoordinates.put(i, currentPos)
                        }
                    }
                }
            }
        } while (!coordsToVisit.isEmpty())

        return LinkedList<Beacon>()
    }

    /**
     * Returns the Queue of beacons that the user should pass to reach the destination the fastest to the calling method
     */
    private fun calcFastestPath(shortestPath: Stack<Coordinate>, destination: Coordinate): Queue<Beacon> {
        var queue = LinkedList<Beacon>()
        var nextOrientation: Orientation
        var m: Direction?
        var temp = shortestPath.pop()

        // While user is not at destination yet
        while (userPos.x != destination.x || userPos.y != destination.y) {
            // If user is already at the current Coordinate from the fastest path, get next Coordinate to move to
            if (userPos.x == temp.x && userPos.y == temp.y) {
                temp = shortestPath.pop()
            }

            // Find out which Orientation the user has to turn to to move to the temp Coordinate
            nextOrientation = getNextOrientation(userPos, currentOrientation, temp)

            // Turn the user's direction to the correct Orientation
            if (userOrientation != nextOrientation) {
                m = getDirectionToTurn(userOrientation, nextOrientation)
            } else {
                m = Direction.FORWARD
            }

            for (i in dbList) {
                if (i.coordinate == Coordinate(temp.x, temp.y)) {
                    queue.add(i)
                }
            }

            updatePosAndOrientation(m!!, 1)
        }
        return queue
    }

    /**
     * Update the position and orientation of the user
     */    private fun updatePosAndOrientation(direction: Direction, numberOfGrid: Int) {
        val newOrdinal = (userOrientation.ordinal + direction.ordinal) % 4
        userOrientation = Orientation.values()[newOrdinal]

        when (userOrientation) {
            Orientation.NORTH -> userPos.y += numberOfGrid
            Orientation.SOUTH -> userPos.y -= numberOfGrid
            Orientation.EAST -> userPos.x += numberOfGrid
            Orientation.WEST -> userPos.x -= numberOfGrid
        }
    }

    /**
     * Returns the next nearest beacon using the KDTree implementation (not actually needed)
     */
    fun findNextNearest(source: Coordinate, destList: ArrayList<Beacon>): Beacon {
        //Not useful beacons
        val beaconList = ArrayList<Beacon>()

        for (p in destList) {
            beaconList.add(p.clone())
        }

        beaconList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        val tree = KDTree(beaconList.size)

        for (i in beaconList) {
            val point = DoubleArray(2)
            point[0] = i.coordinate.x
            point[1] = i.coordinate.y

            tree.add(point)
        }

        val start = DoubleArray(2)
        start[0] = source.x
        start[1] = source.y

        val nextPoint = tree.find_nearest(start)
        val nextCoord = Coordinate(nextPoint.x[0], nextPoint.x[1])

        for (i in beaconList) {
            if (nextCoord == i.coordinate) {
                return i
            }
        }
        return Beacon("Beacon")
    }


    /**
     * Returns the user's position
     */
    fun findUserPos(destList: ArrayList<Beacon>): Coordinate {
        //Not useful beacons
        val beaconList = ArrayList<Beacon>()

        for (p in destList) {
            beaconList.add(p.clone())
        }

        beaconList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        //Definite more than 0.5 in real life. Means inaccurately approximated
        beaconList.removeIf {
            item ->
            item.distance < 0.5
        }

        //Definite more than 0.5 in real life. Means inaccurately approximated
        beaconList.removeIf {
            item ->
            item.distance > 9
        }

        var size = beaconList.size

        var posList = Array(size) {_ -> doubleArrayOf(0.0, 0.0)}
        var distanceList = DoubleArray(size)

        //for testing
//        double[][] positions = new double[][]{{1.0, 1.0}, {1.0, 3.0}, {8.0, 8.0}, {2.0, 2.0}};
//        double[] distances = new double[]{5.0, 5.0, 6.36, 3.9};
//        double[] expectedPosition = new double[]{5.9, 2.0};
//        var testList = ArrayList<Beacon>()
//        var db1 = Beacon("Db1", 1)
//        db1.coordinate = Coordinate(1.0, 1.0)
//        db1.distance = 5.0
//        var db2 = Beacon("Db1", 1)
//        db2.coordinate = Coordinate(1.0, 3.0)
//        db2.distance = 5.0
//        var db3 = Beacon("Db1", 1)
//        db3.coordinate = Coordinate(8.0, 8.0)
//        db3.distance = 6.36
//        var db4 = Beacon("Db1", 1)
//        db4.coordinate = Coordinate(2.0, 2.0)
//        db4.distance = 3.9
//        testList.add(db1)
//        testList.add(db2)
//        testList.add(db3)
//        testList.add(db4)

        if (beaconList.isNotEmpty()) {
            beaconList.sortBy {
                it.distance
            }

            for (i in beaconList) {
                size--
                var pos = doubleArrayOf((i.coordinate.x)*2, (i.coordinate.y)*2)

                distanceList[size] = i.distance
                posList[size] = pos

                if (size == 0) {
                    break
                }
            }
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

        return Coordinate(roundToHalf(res2[0] / 2), roundToHalf(res2[1] / 2))
    }

    /**
     * Returns the approximated distance using the RSSI values
     */
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

    /**
     * Returns the value rounded to half
     */
    private fun roundToHalf(d: Double): Double {
        return Math.round(d * 2) / 2.0
    }
}
