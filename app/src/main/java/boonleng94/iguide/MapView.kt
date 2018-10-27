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
import android.R.attr.radius
import android.graphics.CornerPathEffect
import android.graphics.Typeface





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

            val bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.beacon_gray), gridSize.roundToInt(), gridSize.roundToInt(), true)
            canvas.drawBitmap(bmp, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startX-4, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startY-4, null)
        }


        val corEffect = CornerPathEffect(gridSize/6.toFloat())

        //not last element yet
        var queuePaint = Paint()
        queuePaint.color = Color.GREEN
        queuePaint.strokeWidth = gridSize/2
        queuePaint.style = Paint.Style.STROKE
        queuePaint.pathEffect = corEffect
        queuePaint.alpha = 150

        //not last element yet
        var pathPaint = Paint()
        pathPaint.color = Color.RED
        pathPaint.strokeWidth = gridSize/6
        pathPaint.style = Paint.Style.STROKE
        pathPaint.pathEffect = corEffect
        pathPaint.alpha = 100

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
                queue.lineTo(beacon.coordinate.x.toFloat() * gridSize + gridSize/2, beacon.coordinate.y.toFloat() * gridSize + gridSize/2)
            } else {
                queue.moveTo(beacon.coordinate.x.toFloat() * gridSize + gridSize/2, beacon.coordinate.y.toFloat() * gridSize + gridSize/2)
            }

            if (i == idealQueue.size-1) {
                val endPaint = Paint()
                endPaint.color = Color.GREEN
                endPaint.strokeWidth = gridSize/6
                endPaint.style = Paint.Style.STROKE
                endPaint.alpha = 150
                canvas.drawLine(beacon.coordinate.x.toFloat(), beacon.coordinate.y.toFloat(), beacon.coordinate.x.toFloat() + gridSize, beacon.coordinate.y.toFloat() + gridSize, endPaint)
                canvas.drawLine(beacon.coordinate.x.toFloat(), beacon.coordinate.y.toFloat() + gridSize, beacon.coordinate.x.toFloat() + gridSize, beacon.coordinate.y.toFloat(), endPaint)

                val endTextPaint = Paint()
                endTextPaint.color = Color.BLACK
                endTextPaint.textSize = 50f
                endTextPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("END", beacon.coordinate.x.toFloat() * gridSize - gridSize, beacon.coordinate.y.toFloat() * gridSize + gridSize*2/3, endTextPaint);
            }
        }

        canvas.drawPath(queue, queuePaint)


        var path = Path()

        path.moveTo(startPos.x.toFloat() * gridSize + gridSize/2, startPos.y.toFloat() * gridSize + gridSize/2)

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
            path.lineTo(coord.x.toFloat() * gridSize + gridSize/2, coord.y.toFloat() * gridSize + gridSize/2)
//
//            if (i != 0) {
//                path.lineTo(coord.x.toFloat() * gridSize + gridSize/2, coord.y.toFloat() * gridSize + gridSize/2)
//            } else {
//                path.moveTo(coord.x.toFloat() * gridSize + gridSize/2, coord.y.toFloat() * gridSize + gridSize/2)
//            }
        }

        canvas.drawPath(path, pathPaint)

        var circlePaint = Paint()
        circlePaint.color = Color.RED
        circlePaint.style = Paint.Style.FILL
        circlePaint.alpha = 180

        canvas.drawCircle(startPos.x.toFloat() * gridSize + gridSize/2, startPos.y.toFloat() * gridSize + gridSize/2, gridSize/3, circlePaint)
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