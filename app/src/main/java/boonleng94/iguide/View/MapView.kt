package boonleng94.iguide.View

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import boonleng94.iguide.Controller.Coordinate
import boonleng94.iguide.Model.Beacon
import boonleng94.iguide.R

import kotlin.math.roundToInt

//View class for the Map displaying actual path taken
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

        wallPaint = Paint()
        wallPaint.color = Color.BLACK
        wallPaint.strokeWidth = WallThickness

        unexploredPaint = Paint()
        unexploredPaint.color = ContextCompat.getColor(context!!, R.color.light_gray)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)
        val width = width.toFloat()
        val height = height.toFloat()


        if (width/height < maxX/maxY) {
            gridSize = (width / (maxX + 1))
        } else {
            gridSize = (height / (maxY + 1))
        }

        hMargin = (width - maxX * gridSize) / 2
        vMargin = (height - maxY * gridSize) / 2

        //SET THE MARGIN IN PLACE
        canvas.translate(hMargin, vMargin)

        if (!gridGenerated) {
            generateGrids()
            gridGenerated = true
        }

        drawBorder(canvas)
        drawGrid(canvas)


        //DRAW MAP
        if (mapUpdated) {
            drawMap(canvas)
        }
    }

    //CREATE GRID METHOD
    private fun generateGrids() {
        grids = Array(maxX, { Array<Grid>(maxY, { Grid() }) })

        for (i in 0 until maxX) {
            for (j in 0 until maxY) {
                grids[i][j] = Grid(i * gridSize + gridSize / 30, j * gridSize + gridSize / 30, (i + 1) * gridSize - gridSize / 40, (j + 1) * gridSize - gridSize / 60, unexploredPaint)
            }
        }
    }

    private fun drawBorder(canvas: Canvas) {
        //DRAW BORDER FOR EACH GRID
        for (x in 0 until maxX) {
            for (y in 0 until maxY) {

                canvas.drawLine(
                        x * gridSize,
                        y * gridSize,
                        (x + 1) * gridSize,
                        y * gridSize, wallPaint)
                canvas.drawLine(
                        (x + 1) * gridSize,
                        y * gridSize,
                        (x + 1) * gridSize,
                        (y + 1) * gridSize, wallPaint)
                canvas.drawLine(
                        x * gridSize,
                        y * gridSize,
                        x * gridSize,
                        (y + 1) * gridSize, wallPaint)
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
            canvas.drawBitmap(bmp, grids[i.coordinate.x.toInt()][i.coordinate.y.toInt()].startX-4, grids[i.coordinate.x.toInt()][i.coordinate.y.toInt()].startY-4, null)
        }


        val corEffect = CornerPathEffect(gridSize/6.toFloat())

        var queuePaint = Paint()
        queuePaint.color = Color.GREEN
        queuePaint.strokeWidth = gridSize/2
        queuePaint.style = Paint.Style.STROKE
        queuePaint.pathEffect = corEffect
        queuePaint.alpha = 150

        var pathPaint = Paint()
        pathPaint.color = Color.RED
        pathPaint.strokeWidth = gridSize/6
        pathPaint.style = Paint.Style.STROKE
        pathPaint.pathEffect = corEffect
        pathPaint.alpha = 100

        var queue = Path()

        for ((i, beacon) in idealQueue.withIndex()) {
            Log.d(debugTAG, " idealQueue beacon name = " + beacon.name + " x = " + beacon.coordinate.x + ", y = " + beacon.coordinate.y)

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
            path.lineTo(coord.x.toFloat() * gridSize + gridSize/2, coord.y.toFloat() * gridSize + gridSize/2)
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
}