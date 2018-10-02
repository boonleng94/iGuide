package boonleng94.iguide.Unused

import android.util.Log
import boonleng94.iguide.*
import java.util.*
import kotlin.math.roundToInt

class NavigatorBackup() {
    private lateinit var currentPos: Coordinate

    private lateinit var parentCoordinates: HashMap<Coordinate, Coordinate>

    private lateinit var travelCost: Array<DoubleArray>

    private lateinit var coordsToVisit: ArrayList<Coordinate>
    private lateinit var coordsVisited: ArrayList<Coordinate>

    private lateinit var destination: Coordinate
    private lateinit var currentOrientation: Orientation

    private var destList = ArrayList<DestinationBeacon>()

    private var outputString = StringBuilder("MOVE ")

    //destList = (application as MainApp).destList

    companion object {
        private val LOG_TAG = "NAVIGATOR BACKUP"
        private const val MOVEMENT_COST = 1
        private const val TURNING_COST = 3
    }

    init {
        for (i in 1 until (destination.x-1).roundToInt()) {
            for (j in 1 until (destination.y-1).roundToInt()) {
                travelCost[i][j] = 0.0
                for (d in destList) {
                    coordsToVisit.add(d.coordinate)
                }
            }
        }
        coordsToVisit.add(currentPos)
        travelCost[currentPos.x.roundToInt()][currentPos.y.roundToInt()] = 0.0
    }

    /**
     * Returns true if the given coordinates are within the Arena boundaries
     */
    private fun checkValidCoordinates(y: Int, x: Int): Boolean {
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
                var temp = destination

                while (true) {
                    shortestPathStack.push(temp)

                    temp = parentCoordinates[temp]!!

                    if (temp == null) {
                        break
                    }
                }
                return calcFastestPath(shortestPathStack, destination)
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
