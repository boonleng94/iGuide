package boonleng94.iguide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.roundToInt
import android.graphics.BitmapFactory
import android.graphics.Bitmap

class MapView : View {
    private val debugTAG = "MapView"

    private var maxX: Int = 20
    private var maxY: Int = 20
    private val WallThickness = 2f

    private var gridSize: Float = 0.toFloat()
    private var hMargin:Float = 0.toFloat()
    private var vMargin:Float = 0.toFloat()

    private lateinit var grids: Array<Array<Grid>>

    private var wallPaint: Paint
    private var robotPaint: Paint
    private var waypointPaint: Paint
    private var directionPaint: Paint
    private var emptyPaint: Paint
    private var unexploredPaint: Paint
    private var ftpPaint: Paint
    private var gridNumberPaint: Paint

    private var gridGenerated = false
    private var mapUpdated = false

    private lateinit var destList: ArrayList<DestinationBeacon>
    private lateinit var startPos: Coordinate
    private lateinit var idealQueue: ArrayList<DestinationBeacon>
    private lateinit var pathTaken : ArrayList<Coordinate>

    constructor(context: Context) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)

        //PAINT THE THICKNESS OF THE WALL
        wallPaint = Paint()
        wallPaint.color = Color.BLACK
        wallPaint.strokeWidth = WallThickness

        //COLOR FOR ROBOT
        robotPaint = Paint()
        robotPaint.color = Color.GREEN

        //COLOR FOR ROBOT DIRECTION
        directionPaint = Paint()
        directionPaint.color = Color.BLACK

        //COLOR FOR WAY POINT
        waypointPaint = Paint()
        waypointPaint.color = Color.YELLOW

        //COLOR FOR EXPLORED BUT EMPTY
        emptyPaint = Paint()
        emptyPaint.color = Color.WHITE

        //COLOR FOR UNEXPLORED PATH
        unexploredPaint = Paint()
        unexploredPaint.color = ContextCompat.getColor(context!!, R.color.icy_marshmallow)

        gridNumberPaint = Paint()
        gridNumberPaint.color = Color.BLACK
        gridNumberPaint.textSize = 18f
        gridNumberPaint.typeface = Typeface.DEFAULT_BOLD

        //COLOR FOR FASTEST PATH
        ftpPaint = Paint()
        ftpPaint.color = Color.parseColor("#FFC0CB")
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(debugTAG, "Painting now!!")

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
            Log.d(debugTAG, "ttt")
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
//        if (!mapUpdated) {
//            drawMap(canvas)
//        }
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

    //DRAW NUMBERS ON MAP GRID
    private fun drawGridNumber(canvas: Canvas) {

        //GRID NUMBER FOR ROW
        for (x in 0..maxX-1) {
            if (x > 9 && x < 15) {
                canvas.drawText(Integer.toString(x), grids[x][19].startX + gridSize / 5, grids[x][19].endY + gridSize / 1.5f, gridNumberPaint)
            } else {
                //GRID NUMBER FOR ROW
                canvas.drawText(Integer.toString(x), grids[x][19].startX + gridSize / 3, grids[x][19].endY + gridSize / 1.5f, gridNumberPaint)
            }
        }

        //GRID NUMBER FOR COLUMN
        for (x in 0..maxY-1) {
            if (x > 9 && x < 20) {
                canvas.drawText(Integer.toString(19 - x), grids[0][x].startX - gridSize / 1.5f, grids[0][x].endY - gridSize / 3.5f, gridNumberPaint)
            } else {
                canvas.drawText(Integer.toString(19 - x), grids[0][x].startX - gridSize / 1.2f, grids[0][x].endY - gridSize / 3.5f, gridNumberPaint)

            }
        }
    }

    //DRAW MAP ON THE CANVAS
    private fun drawMap(canvas: Canvas) {
//        if (wayPointRow != -1 && wayPointCols != -1) {
//            canvas.drawRect(cells[wayPointCols][wayPointRow].startX, cells[wayPointCols][wayPointRow].startY, cells[wayPointCols][wayPointRow].endX, cells[wayPointCols][wayPointRow].endY, waypointPaint)
//        }

        for (i in destList) {
            //draw beacons
            val bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.move_forward), gridSize.roundToInt()-4, gridSize.roundToInt()-4, true);
            canvas.drawBitmap(bmp, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startX-4, grids[i.coordinate.x.roundToInt()][i.coordinate.y.roundToInt()].startY-4, null)        }

        for (i in idealQueue) {

        }

        for (i in pathTaken) {

        }
    }

    fun updateMap(destList: ArrayList<DestinationBeacon>, startPos: Coordinate, idealQueue: ArrayList<DestinationBeacon>, pathTaken: ArrayList<Coordinate>) {
        this.destList = destList
        this.startPos = startPos
        this.idealQueue = idealQueue
        this.pathTaken = pathTaken

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