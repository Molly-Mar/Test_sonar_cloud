package com.padc.cardscanner_mtkm

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var ocrImage: ImageView
    lateinit var ocrImage1: ImageView
    var flag = ""

    //capture
    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ocrImage = findViewById(R.id.ocrImageView)
        ocrImage1 = findViewById(R.id.ocrImageView1)

        hideIv()
        setUpListener()

    }

    fun setUpListener() {
        //select
        //set an onclick listener on the button to trigger the @pickImage() method
        selectImageBtn.setOnClickListener {
            showSelectIv()
            flag = "select"
            pickImage()
        }


        //capture
        //button click
        btn_capture.setOnClickListener {
            showCaptureIv()
            flag = "capture"
            //if system os is Marshmallow or Above, we need to request runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED
                ) {
                    //permission was not enabled
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    //permission already granted
                    openCamera()
                }
            } else {
                //system os is < marshmallow
                openCamera()
            }
        }


        //set an onclick listener on the button to trigger the @processImage method
        processImageBtn.setOnClickListener {
            processImage(processImageBtn)
        }
    }

    //select
    fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //select
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            ocrImage1.setImageURI(data!!.data)
        }

        //capture
        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK) {
            //set image captured to image view
            ocrImage.setImageURI(image_uri)
        }
    }

    fun processImage(v: View) {
        if (flag == "capture") {
            if (ocrImage.drawable != null) {
                ocrResultEt.setText("")
                v.isEnabled = false
                val bitmap = (ocrImage.drawable as BitmapDrawable).bitmap
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

                detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        v.isEnabled = true
                        processResultText(firebaseVisionText)
                    }
                    .addOnFailureListener {
                        v.isEnabled = true
                        ocrResultEt.setText(getString(R.string.failed))
                    }
            } else {
                Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
            }
        } else if (flag == "select") {
            if (ocrImage1.drawable != null) {
                ocrResultEt.setText("")
                v.isEnabled = false
                val bitmap = (ocrImage1.drawable as BitmapDrawable).bitmap
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

                detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        v.isEnabled = true
                        processResultText(firebaseVisionText)
                    }
                    .addOnFailureListener {
                        v.isEnabled = true
                        ocrResultEt.setText(getString(R.string.failed))
                    }
            } else {
                Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            ocrResultEt.setText(getString(R.string.no_text_found))
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            ocrResultEt.append(blockText + "\n")
        }
    }

    //capture
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup was granted
                    openCamera()
                } else {
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showSelectIv() {
        ocrImage1.visibility = View.VISIBLE
        ocrImage.visibility = View.INVISIBLE
        ocrResultEt.text = ""
    }

    private fun showCaptureIv() {
        ocrImage.visibility = View.VISIBLE
        ocrImage1.visibility = View.INVISIBLE
        ocrResultEt.text = ""
    }

    private fun hideIv() {
        ocrImage1.visibility = View.INVISIBLE
        ocrImage.visibility = View.VISIBLE
    }

}
