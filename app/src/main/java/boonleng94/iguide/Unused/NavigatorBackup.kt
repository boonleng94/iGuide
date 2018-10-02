package boonleng94.iguide.Unused

import android.util.Log
import boonleng94.iguide.*
import java.util.*
import kotlin.math.roundToInt

class NavigatorBackup(currentPos: Coordinate, currentOrientation: Orientation, destination: Coordinate, destList: ArrayList<DestinationBeacon>) {
    var currentPos = currentPos
    val destination = destination
    var currentOrientation = currentOrientation
    var destList = destList

    private var parentCoordinates = HashMap<Coordinate, Coordinate>()

    private lateinit var travelCost: Array<IntArray>

    private var coordsToVisit = ArrayList<Coordinate>()
    private var coordsVisited = ArrayList<Coordinate>()
    private var outputString = StringBuilder("MOVE ")

    private lateinit var neighbourCoordinates: Array<Coordinate>

    companion object {
        private val LOG_TAG = "NAVIGATOR BACKUP"
        private const val MOVEMENT_COST = 1
        private const val TURNING_COST = 3
        private const val MAX_COST = 99999
    }

    fun initialize() {
        destList.removeIf {
            item ->
            item.coordinate == Coordinate(-1.0, -1.0)
        }

        neighbourCoordinates = Array(4, {Coordinate(0.0,0.0)})

        var maxX = destList[0].coordinate.x
        var maxY = destList[0].coordinate.y

        for (i in destList) {
            if (i.coordinate.x > maxX) {
                maxX = i.coordinate.x
            }
            if (i.coordinate.y > maxY) {
                maxY = i.coordinate.y
            }
        }
        travelCost = Array(maxX.toInt(), { IntArray(maxY.toInt()) })

        for (i in 1 until (destination.x-1).roundToInt()) {
            for (j in 1 until (destination.y-1).roundToInt()) {
                travelCost[i][j] = MAX_COST
            }
        }

        for (i in 1 until (destination.x-1).roundToInt()) {
            for (j in 1 until (destination.y-1).roundToInt()) {
                for (d in destList) {
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
     * Returns true if the given coordinates are within the Arena boundaries
     */
    private fun checkValidCoordinates(y: Double, x: Double): Boolean {
        return y >= 0 && x >= 0 && y < destination.y && x < destination.x
    }

    /**
     * Returns the first optimal cell that can be traversed to reach the Goal (lowest cost - to ensure shortest path)
     */
    private fun optimalCoordinate(destination: Coordinate): Coordinate? {
        val size = coordsToVisit.size
        var minCost = 99.00
        var result: Coordinate? = null

        // gCost is the cost of the path from a Coordinate to GOAL
        for (i in size - 1 downTo 0) {
            val gCost = travelCost[coordsToVisit[i].x.roundToInt()][coordsToVisit[i].y.roundToInt()]
            val cost = gCost + heuristicCost(coordsToVisit[i], destination)
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
    private fun getNextOrientation(userCurrentCoordinate: Coordinate?, userCurrentOrientation: Orientation, targetCoordinate: Coordinate): Orientation {
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
     * Returns the moving/travel cost required for the mRobot to move from origin Coordinate to destination Coordinate(assuming both ccoordinates are neighbours of each other).
     */
    private fun getMoveCost(originCoordinate: Coordinate, destinationCoordinate: Coordinate, orientation: Orientation): Double {
        val moveCost = MOVEMENT_COST.toDouble() // one movement to neighbor

        val targetDir = getNextOrientation(originCoordinate, orientation, destinationCoordinate)

        var numOfTurn = Math.abs(orientation.ordinal - targetDir.ordinal)
        if (numOfTurn > 2) {
            numOfTurn %= 2
        }
        val turnCost = (numOfTurn * TURNING_COST).toDouble()

        return moveCost + turnCost
    }

    /**
     * Returns the heuristic cost needed to traverse from a given Coordinate to the Goal
     * Heuristics cost - estimates the cost of the cheapest path from Coordinate to the GOAL. (time taken)
     * Wiki - For the algorithm to find the actual shortest path, the heuristic function must be admissible,
     * meaning that it never overestimates the actual cost to get to the nearest destination node.
     */
    private fun heuristicCost(coordinate: Coordinate, destination: Coordinate): Double {
        // The actual number of moves required (difference in the rows and xumns)
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
     * Returns the Direction the mRobot needs to turn to face the next Orientation from the original Orientation
     */
    private fun getDirectionToTurn(originalOrientation: Orientation, nextOrientation: Orientation): Direction? {
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
     * Returns the String (sequence of Movements) that the mRobot should take to reach the Goal the fastest
     * This method is for main program to call
     */
    fun executeFastestPath(): String? {
        Log.i(LOG_TAG, "Fastest path from (" + currentPos.x + ", " + currentPos.y + ") to (" + destination.x + ", " + destination.y + ")...")
        // Stack to contain the shortest path/mandatory cells the mRobot should take (for backtracking later)
        val shortestPathStack = Stack<Coordinate>()

        do {
            // Get the Coordinate with the smallest cost from the array of Coordinates to visit and assign it as the current Coordinate
            currentPos = optimalCoordinate(destination)!!
            //Log.i(LOG_TAG, "Current cell = (" + currentPos.x + ", " + currentPos.y + ")");

            // Change the Orientation of the mRobot to face the current Coordinate from the previous Coordinate
            if (parentCoordinates.containsKey(currentPos)) {
                currentOrientation = getNextOrientation(parentCoordinates[currentPos], currentOrientation, currentPos)
            }

            // Add current Coordinate to array of visited Coordinates and remove from array of Coordinates to visit
            coordsVisited.add(currentPos)
            coordsToVisit.remove(currentPos)

            // If the Goal cell is in the array of visited Coordinates - a path to the Goal cell has been found, find the fastest path
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

            //Log.i(LOG_TAG, "Current cell: " + currentPos.x + " " + currentPos.y);

            // Store the neighbour cells of the current Coordinate into the array (Up, down, left, right) - null if the respective neighbour cell is an obstacle
            // Up neighbour cell
            if (checkValidCoordinates(currentPos.y + 1, currentPos.x)) {
                neighbourCoordinates[0] = Coordinate(currentPos.x, currentPos.y + 1)
            }
            // Down neighbour cell
            if (checkValidCoordinates(currentPos.y - 1, currentPos.x)) {
                neighbourCoordinates[1] = Coordinate(currentPos.x, currentPos.y - 1)
            }
            // Left neighbour cell
            if (checkValidCoordinates(currentPos.y, currentPos.x - 1)) {
                neighbourCoordinates[2] = Coordinate(currentPos.x - 1, currentPos.y)
            }
            // Right neighbour cell
            if (checkValidCoordinates(currentPos.y, currentPos.x + 1)) {
                neighbourCoordinates[3] = Coordinate(currentPos.x + 1, currentPos.y)
            }

            // For-loop to iterate through the neighbour Coordinates and update the travel cost of each Coordinate
            for (i in neighbourCoordinates.indices) {
                if (neighbourCoordinates[i] != null) {
                    // The neighbour cell has already been visited previously
                    if (coordsVisited.contains(neighbourCoordinates[i])) {
                        continue
                    }

                    if (!coordsToVisit.contains(neighbourCoordinates[i])) {
                        parentCoordinates.put(neighbourCoordinates[i], currentPos)
                        travelCost[neighbourCoordinates[i].x.toInt()][neighbourCoordinates[i].y.toInt()] = travelCost[currentPos.x.toInt()][currentPos.y.toInt()] + getMoveCost(currentPos, neighbourCoordinates[i], currentOrientation).toInt()
                        coordsToVisit.add(neighbourCoordinates[i])
                    } else {
                        val currentCost = travelCost[neighbourCoordinates[i].x.toInt()][neighbourCoordinates[i].y.toInt()]
                        val updatedCost = travelCost[currentPos.x.toInt()][currentPos.y.toInt()] + getMoveCost(currentPos, neighbourCoordinates[i], currentOrientation)
                        if (updatedCost < currentCost) {
                            travelCost[neighbourCoordinates[i].x.toInt()][neighbourCoordinates[i].y.toInt()] = updatedCost.toInt()
                            parentCoordinates.put(neighbourCoordinates[i], currentPos)
                        }
                    }
                }
            }
        } while (!coordsToVisit.isEmpty())

        return null
    }

    /**
     * Returns the String (sequence of Movements) that the mRobot should take to reach the Goal the fastest as well as execute it
     */
    private fun calcFastestPath(shortestPath: Stack<Coordinate>, destination: Coordinate): String {
        var nextOrientation: Orientation
        var m: Direction?
        val movementsToTake = ArrayList<Direction>()
        var temp = shortestPath.pop()
        var forwardCounter = 0

        // While mRobot is not at Goal yet
        while (currentPos.x != destination.x || currentPos.y != destination.y) {
            // If mRobot is already at the current Coordinate from the fastest path, get next Coordinate to move to
            if (currentPos.x == temp.x && currentPos.y == temp.y) {
                temp = shortestPath.pop()
            }

            // Find out which Orientation the mRobot has to turn to to move to the temp Coordinate
            nextOrientation = getNextOrientation(currentPos, currentOrientation, temp)

            // Turn the mRobot's direction to the correct Orientation
            if (currentOrientation !== nextOrientation) {
                m = getDirectionToTurn(currentOrientation, nextOrientation)
            } else {
                m = Direction.FORWARD
            }

            Log.i(LOG_TAG, "User move " + m.toString() + " from (" + currentPos.x + ", " + currentPos.y + ") to (" + temp.x + ", " + temp.y + ")")

            updatePosAndOrientation(m!!,1)

            if (m === Direction.FORWARD) {
                forwardCounter++
            } else if (m === Direction.RIGHT) {
                if (forwardCounter != 0) {
                    outputString.append("f$forwardCounter,")
                    forwardCounter = 0
                }
                outputString.append("Right,")
            } else if (m === Direction.LEFT) {
                if (forwardCounter != 0) {
                    outputString.append("f$forwardCounter,")
                    forwardCounter = 0
                }
                outputString.append("Left,")
            } else if (m === Direction.BACK) {
                if (forwardCounter != 0) {
                    outputString.append("f$forwardCounter,")
                    forwardCounter = 0
                }
                outputString.append("Back,")
            }

            //outputString.append(Direction.toString(m)+"|");

            //If rotate, add a step forward after rotation
            if (m === Direction.RIGHT || m === Direction.LEFT || m === Direction.BACK) {
                movementsToTake.add(Direction.FORWARD)
                forwardCounter++
            }
        }

        if (forwardCounter != 0) {
            outputString.append("f$forwardCounter,")
        }

        return outputString.toString()
    }

    private fun updatePosAndOrientation(direction: Direction, numberOfGrid: Int) {
        val newOrdinal = (currentOrientation.ordinal + direction.ordinal) % 4
        currentOrientation = Orientation.values()[newOrdinal]

        when (currentOrientation) {
            Orientation.NORTH -> currentPos.y += numberOfGrid
            Orientation.SOUTH -> currentPos.y -= numberOfGrid
            Orientation.EAST -> currentPos.x += numberOfGrid
            Orientation.WEST -> currentPos.x -= numberOfGrid
        }
    }
}
