package com.weekend.drawingboard

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener,
    DrawingBoardView.Callback, Handler.Callback {
    private var mUndoView: View? = null
    private var mRedoView: View? = null
    private var mPenView: View? = null
    private var mEraserView: View? = null
    private var mClearView: View? = null
    private var mDrawingBoardView: DrawingBoardView? = null
    private var mSaveProgressDlg: ProgressDialog? = null
    private var mHandler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDrawingBoardView = findViewById<View>(R.id.palette) as DrawingBoardView
        mDrawingBoardView!!.setCallback(this)
        mUndoView = findViewById(R.id.undo)
        mRedoView = findViewById(R.id.redo)
        mPenView = findViewById(R.id.pen)
        mPenView?.isSelected = true
        mEraserView = findViewById(R.id.eraser)
        mClearView = findViewById(R.id.clear)
        mUndoView?.setOnClickListener(this)
        mRedoView?.setOnClickListener(this)
        mPenView?.setOnClickListener(this)
        mEraserView?.setOnClickListener(this)
        mClearView?.setOnClickListener(this)
        mUndoView?.isEnabled = false
        mRedoView?.isEnabled = false
        mHandler = Handler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler!!.removeMessages(MSG_SAVE_FAILED)
        mHandler!!.removeMessages(MSG_SAVE_SUCCESS)
    }

    private fun initSaveProgressDlg() {
        mSaveProgressDlg = ProgressDialog(this)
        mSaveProgressDlg!!.setMessage("正在保存,请稍候...")
        mSaveProgressDlg!!.setCancelable(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_SAVE_FAILED -> {
                mSaveProgressDlg!!.dismiss()
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
            MSG_SAVE_SUCCESS -> {
                mSaveProgressDlg!!.dismiss()
                Toast.makeText(this, "画板已保存", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> {
                if (mSaveProgressDlg == null) {
                    initSaveProgressDlg()
                }
                mSaveProgressDlg!!.show()
                Thread {
                    val bm = mDrawingBoardView!!.buildBitmap()
                    val savedFile = saveImage(bm, 100)
                    if (savedFile != null) {
                        scanFile(this@MainActivity, savedFile)
                        mHandler!!.obtainMessage(MSG_SAVE_SUCCESS)
                            .sendToTarget()
                    } else {
                        mHandler!!.obtainMessage(MSG_SAVE_FAILED)
                            .sendToTarget()
                    }
                }.start()
            }
        }
        return true
    }

    override fun onUndoRedoStatusChanged() {
        mUndoView!!.isEnabled = mDrawingBoardView!!.canUndo()
        mRedoView!!.isEnabled = mDrawingBoardView!!.canRedo()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.undo -> mDrawingBoardView!!.undo()
            R.id.redo -> mDrawingBoardView!!.redo()
            R.id.pen -> {
                v.isSelected = true
                mEraserView!!.isSelected = false
                mDrawingBoardView!!.mode = DrawingBoardView.Mode.DRAW
            }
            R.id.eraser -> {
                v.isSelected = true
                mPenView!!.isSelected = false
                mDrawingBoardView!!.mode = DrawingBoardView.Mode.ERASER
            }
            R.id.clear -> mDrawingBoardView!!.clear()
        }
    }

    companion object {
        private const val MSG_SAVE_SUCCESS = 1
        private const val MSG_SAVE_FAILED = 2
        private fun scanFile(context: Context, filePath: String) {
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            scanIntent.data = Uri.fromFile(File(filePath))
            context.sendBroadcast(scanIntent)
        }

        private fun saveImage(bmp: Bitmap?, quality: Int): String? {
            if (bmp == null) {
                return null
            }
            val appDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    ?: return null
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            val file = File(appDir, fileName)
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                fos.flush()
                return file.absolutePath
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }
    }
}