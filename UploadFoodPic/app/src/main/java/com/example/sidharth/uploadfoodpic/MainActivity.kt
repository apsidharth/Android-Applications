package com.example.sidharth.uploadfoodpic

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.Tag
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.btnChoose
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PICK_IMAGE_REQUEST = 12
    private val CAMERA_REQUEST_CODE = 1
    lateinit var imageFilePath: String

    private var filePath: Uri?=null
    var photoURI: Uri?=null


    internal var storage:FirebaseStorage?= null
    internal var storageReference:StorageReference?=null


    override fun onClick(v: View?) {
        if(v === btnChoose)
            showFileChooser()
        else if (v === btnUpload)
            uploadFile()
        else if (v === btnCamera)
            onLaunchCamera()

    }

    private fun showFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"SELECT PICTURE"), PICK_IMAGE_REQUEST)

    }

    private fun uploadFile() {
        if(filePath != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()

            val imageRef = storageReference!!.child("images/" + UUID.randomUUID().toString())
            imageRef.putFile(filePath!!).addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, "Uploaded. Thanks for your contribution!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener() {
                        progressDialog.dismiss()
                        Toast.makeText(applicationContext, "Failed", Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                        progressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                    }

        }

    }
    private fun onLaunchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //Ensure there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                //log error
               // Log.e(Tag, e.toString())
            }
            //continue only if file was successfully created!
            if (photoFile != null) {
                 photoURI = FileProvider.getUriForFile(this,
                        "com.example.sidharth.uploadfoodpic.fileprovider",
                        photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

       when(requestCode) {

           PICK_IMAGE_REQUEST -> {
               if(resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                   filePath = data.data
                   try {
                       val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                       imageView!!.setImageBitmap(bitmap)
                   }catch (e:IOException) {
                       e.printStackTrace()
                   }
               }
           }
           CAMERA_REQUEST_CODE -> {

               if(resultCode == Activity.RESULT_OK  ) {

                   val progressDialog =ProgressDialog(this)
                   progressDialog.setTitle("Uploading...")
                   progressDialog.show()

                   val imageRef = storageReference!!.child("images/" + UUID.randomUUID().toString())
                         //  .child(photoURI!!.lastPathSegment)

                   imageRef.putFile(photoURI!!).addOnSuccessListener {
                       progressDialog.dismiss()
                       Toast.makeText(applicationContext, "Uploaded. Thanks for your contribution!", Toast.LENGTH_SHORT).show()
                   }
                           .addOnFailureListener() {
                               progressDialog.dismiss()
                               Toast.makeText(applicationContext, "Failed. Please make sure your Internet connection is active", Toast.LENGTH_SHORT).show()
                           }
                           .addOnProgressListener { taskSnapshot ->
                               val progress = 100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                               progressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                           }

                   }
               }
           }
    }
    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timestamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if(!storageDir.exists()) storageDir.mkdirs()
        val imageFile = createTempFile(imageFileName, ".jpg", storageDir)
        imageFilePath = imageFile.absolutePath
        return imageFile
    }
    fun setScaledBitmap(): Bitmap {
        val imageWidth = imageView.width
        val imageHeight = imageView.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFilePath, bmOptions)
        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight

        val scaleFactor = Math.min(bitmapWidth / imageWidth, bitmapHeight / imageHeight)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(imageFilePath, bmOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initializing Firebase
        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference

        btnChoose.setOnClickListener(this)
        btnUpload.setOnClickListener(this)
        btnCamera.setOnClickListener(this)


    }
}
