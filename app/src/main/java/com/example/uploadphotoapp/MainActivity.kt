package com.example.uploadphotoapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelectPhoto: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var imgSelectedPhoto: ImageView
    private lateinit var btnUploadPhoto: Button
    private lateinit var btnViewGallery: Button

    private lateinit var storageReference: StorageReference
    private var selectedPhotoUri: Uri? = null
    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        imgSelectedPhoto = findViewById(R.id.imgSelectedPhoto)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        btnViewGallery = findViewById(R.id.btnViewGallery)

        storageReference = FirebaseStorage.getInstance().reference

        btnSelectPhoto.setOnClickListener {
            if (checkAndRequestPermissions()) {
                selectPhoto()
            }
        }

        btnTakePhoto.setOnClickListener {
            if (checkAndRequestPermissions()) {
                takePhoto()
            }
        }

        btnUploadPhoto.setOnClickListener {
            uploadPhoto()
        }

        btnViewGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(it)
            }
        }
        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 0)
            false
        } else {
            true
        }
    }

    private fun selectPhoto() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, 2)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1 -> {
                    selectedPhotoUri = data?.data
                    selectedPhotoUri?.let { uri ->
                        imgSelectedPhoto.setImageURI(uri)
                        saveImageToInternalStorage(uri)
                    }
                }
                2 -> {
                    val file = File(currentPhotoPath)
                    selectedPhotoUri = Uri.fromFile(file)
                    imgSelectedPhoto.setImageURI(selectedPhotoUri)
                    saveImageToInternalStorage(selectedPhotoUri!!)
                }
            }
        }
    }

    private fun uploadPhoto() {
        if (selectedPhotoUri != null) {
            val fileReference = storageReference.child("uploads/${System.currentTimeMillis()}.jpg")
            val uploadTask = fileReference.putFile(selectedPhotoUri!!)

            uploadTask.addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show()
                    imgSelectedPhoto.setImageURI(null)
                    selectedPhotoUri = null
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: FileOutputStream? = null
        try {
            fos = openFileOutput(filename, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
            return filename
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
