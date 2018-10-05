package boonleng94.iguide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle

import android.support.v7.app.AppCompatActivity
import android.view.View

class MainMapActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapCanvas = MapCanvas(this)
        val destList = (application as MainApp).destList
    }

    class MapCanvas(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            canvas.drawRGB(255, 255, 0)
            val width = getWidth()
            val paint = Paint()
            paint.setARGB(255, 255, 0, 0)
            canvas.drawLine(0f, 30f, width.toFloat(), 30f, paint)
            paint.setStrokeWidth(4f)
            canvas.drawLine(0f, 60f, width.toFloat(), 60f, paint)
        }
    }
}