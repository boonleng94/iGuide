package boonleng94.iguide

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.roundToInt

class MapView : View {
    private val debugTAG = "MapView"

    private var maxX: Int = 1
    private var maxY: Int = 1
    private val WallThickness = 2f

    private var gridSize: Float = 0.toFloat()
    private var hMargin:Float = 0.toFloat()
    private var vMargin:Float = 0.toFloat()

    private lateinit var grids: Array<Array<Grid>>

    private var wallPaint: Paint
    private var emptyPaint: Paint
    private var unexploredPaint: Paint
    private var gridGenerated = false
    private var mapUpdated = false

    private lateinit var beaconList: ArrayList<Beacon>
    private lateinit var startPos: Coordinate
    private lateinit var idealQueue: ArrayList<Beacon>
    private lateinit var pathTaken : ArrayList<Coordinate>

    constructor(context: Context) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)

        //PAINT THE THICKNESS OF THE WALL
        wallPaint = Paint()
        wallPaint.color = Color.BLACK
        wallPaint.strokeWidth = WallThickness

        //COLOR FOR EXPLORED BUT EMPTY
        emptyPaint = Paint()
        emptyPaint.color = Color.WHITE

        //COLOR FOR UNEXPLORED PATH
        unexploredPaint = Paint()
        unexploredPaint.color = ContextCompat.getColor(context!!, R.color.icy_marshmallow)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //BACKGROUND COLOR OF CANVAS
        canvas.drawColor(Color.WHITE)
        //WIDTH OF THE CANVAS
        val width = width.toFloat()
        //HEIGHT OF THE CANVAS
        val height = height.toFloat()

        Log.d(debugTAG, "height: $height, width: $width")

        //CALCULATE THE CELLSIZE BASED ON THE DIMENSIONS OF THE CANVAS
        if (width/height < maxX/maxY) {
            gridSize = (width / (maxX + 1))
        } else {
            gridSize = (height / (maxY + 1))
        }

        //CALCULATE MARGIN SIZE FOR THE CANVAS
        hMargin = (width - maxX * gridSize) / 2
        vMargin = (height - maxY * gridSize) / 2

        Log.d(debugTAG, "gridsize: $gridSize")

        Log.d(debugTAG, "hMargin: $hMargin, vMargin: $vMargin")

        //SET THE MARGIN IN PLACE
        canvas.translate(hMargin, vMargin)

        if (!gridGenerated) {
            generateGrids()
            gridGenerated = true
        }

        //DRAW BORDER FOR EACH GRID
        drawBorder(canvas)

        //DRAW EACH INDIVIDUAL GRID
        drawGrid(canvas)

        //DRAW GRID NUMBER
        //drawGridNumber(canvas)

        //canvas.rotate(180f,canvas.getWidth()/2.toFloat(),canvas.getHeight()/2.toFloat())

        //DRAW MAP
        if (mapUpdated) {
            drawMap(canvas)
        }
    }

    //CREATE GRID METHOD
    private fun generateGrids() {
        grids = Array(maxX, { Array<Grid>(maxY, {Grid()}) })

        for (i in 0 until maxX) {
            for (j in 0 until maxY) {
                grids[i][j] = Grid (i*gridSize+gridSize/30, j*gridSize+gridSize/30, (i+1)*gridSize-gridSize/40, (j+1)*gridSize-gridSize/60, unexploredPaint)
            }
        }
    }

    private fun drawBorder(canvas: Canvas) {
        //DRAW BORDER FOR EACH GRID
        for (x in 0 until maxX) {
            for (y in 0 until maxY) {

                //DRAW LINE FOR TOPWALL OF GRID
                canvas.drawLine(
                        x * gridSize,
                        y * gridSize,
                        (x + 1) * gridSize,
                        y * gridSize, wallPaint)
                //DRAW LINE FOR RIGHTWALL OF GRID
                canvas.drawLine(
                        (x + 1) * gridSize,
                        y * gridSize,
                        (x + 1) * gridSize,
                        (y + 1) * gridSize, wallPaint)
                //DRAW LINE FOR LEFTWALL OF GRID
                canvas.drawLine(
                        x * gridSize,
                        y * gridSize,
                        x * gridSize,
                        (y + 1) * gridSize, wallPaint)
                //DRAW LINE FOR BOTTOMWALL OF GRID
                canvas.drawLine(
                        x * gridSize,
                        (y + 1) * gridSize,
                        (x + 1) * gridSize,
                        (y + 1) * gridSize, wallPaint)
            }
        }
    }

    //DRAW INDIVIDUAL GRID
    private fun drawGrid(canvas: Canvas) {
        for (x in 0 until maxX) {
            for (y in 0 until maxY) {
                //DRAW EACH INDIVIDUAL GRID
                canvas.drawRect(grids[x][y].startX, grids[x][y].startY, grids[x][y].endX, grids[x][y].endY, grids[x][y].paint)

            }
        }
    }

    //DRAW MAP ON THE CANVAS
    private fun drawMap(canvas: Canvas) {
        Handler().postDelayed({

        }, 5000)

        for (i in beaconList) {
            //draw beacons
            Log.d(debugTAG, " bList beacon name = " + i.name + " x = " + i.coordinate.x + ", y = " + i.coordinate.y)

            val bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.move_forward), gridSize.roundToInt()-4, gridSize.roundToInt()-4, true)
            canvas.drawBitmap(bmp, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startY-4, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startY-4, null)
        }

        //not last element yet
        var queuePaint = Paint()
        queuePaint.color = Color.GREEN
        queuePaint.strokeWidth = gridSize/2
        queuePaint.style = Paint.Style.STROKE


        //not last element yet
        var pathPaint = Paint()
        pathPaint.color = Color.RED
        pathPaint.strokeWidth = gridSize/2
        pathPaint.style = Paint.Style.STROKE

        var queue = Path()

        for ((i, beacon) in idealQueue.withIndex()) {
            Log.d(debugTAG, " idealQueue beacon name = " + beacon.name + " x = " + beacon.coordinate.x + ", y = " + beacon.coordinate.y)

//            if (i != idealQueue.size - 1) {
//                canvas.drawLine(
//                        beacon.coordinate.x.toFloat() * gridSize,
//                        beacon.coordinate.y.toFloat() * gridSize,
//                        idealQueue[i + 1].coordinate.x.toFloat() * gridSize,
//                        idealQueue[i + 1].coordinate.y.toFloat() * gridSize,
//                        queuePaint)
//            }
            if (i != 0) {
                queue.lineTo(beacon.coordinate.x.toFloat() * gridSize, beacon.coordinate.y.toFloat() * gridSize)
            } else {
                queue.moveTo(beacon.coordinate.x.toFloat() * gridSize, beacon.coordinate.y.toFloat() * gridSize)
            }
        }

        canvas.drawPath(queue, queuePaint)


        var path = Path()

        for ((i, coord) in pathTaken.withIndex()) {
            Log.d(debugTAG, " pathTaken name = x = " + coord.x + ", y = " + coord.y)

//            if (i != pathTaken.size - 1) {
//                canvas.drawLine(
//                        coord.x.toFloat() * gridSize + gridSize / 4,
//                        coord.y.toFloat() * gridSize + gridSize / 4,
//                        pathTaken[i + 1].x.toFloat() * gridSize + gridSize / 4,
//                        pathTaken[i + 1].y.toFloat() * gridSize + gridSize / 4,
//                        pathPaint)
//            }
            if (i != 0) {
                path.lineTo(coord.x.toFloat() * gridSize, coord.y.toFloat() * gridSize)
            } else {
                path.moveTo(coord.x.toFloat() * gridSize, coord.y.toFloat() * gridSize)
            }
        }

        canvas.drawPath(path, pathPaint)
    }

    fun updateMap(beaconList: ArrayList<Beacon>, startPos: Coordinate, idealQueue: ArrayList<Beacon>, pathTaken: ArrayList<Coordinate>) {
        this.startPos = startPos
        this.idealQueue = idealQueue
        this.pathTaken = pathTaken
        this.beaconList = beaconList

        for (i in beaconList) {
            if (i.coordinate.x > maxX) {
                maxX = i.coordinate.x.toInt()
            }
            if (i.coordinate.y > maxY) {
                maxY = i.coordinate.y.toInt()
            }
        }

        maxX+=1
        maxY+=1

        mapUpdated = true
        invalidate()
    }

    private class Grid() {
        var startX: Float = 0.toFloat()
        var startY:Float = 0.toFloat()
        var endX:Float = 0.toFloat()
        var endY:Float = 0.toFloat()
        var paint: Paint = Paint()

        constructor (startX: Float, startY: Float, endX: Float, endY: Float, paint: Paint): this() {
            this.startX = startX
            this.startY = startY
            this.endX = endX
            this.endY = endY
            this.paint = paint
        }
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}