package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.drjacky.imagepicker.ImagePicker
import com.karumi.dexter.BuildConfig
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.theapache64.removebg.RemoveBg
import com.theapache64.removebg.utils.ErrorResponse
import com.theapache64.twinkill.logger.info
import java.io.File
import java.lang.StringBuilder
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Environment
import java.io.FileOutputStream
import java.lang.Exception
import android.media.MediaScannerConnection

import android.os.Build
import android.util.Log
import androidx.core.net.toFile
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.BitmapFactory
import android.widget.Toast
import com.squareup.okhttp.ConnectionSpec
import com.squareup.okhttp.OkHttpClient
import java.util.*


class MainActivity : AppCompatActivity() {


    private var inputImage: File? = null
    private var uri: Uri? = null
    private var gallery = true
    @BindView(R.id.iv_input)
    lateinit var ivInput: ImageView


    @BindView(R.id.tv_input_details)
    lateinit var tvInputDetails: TextView

    @BindView(R.id.b_process)
    lateinit var bProcess: View

    @BindView(R.id.tv_progress)
    lateinit var tvProgress: TextView

    @BindView(R.id.pb_progress)
    lateinit var pbProgress: ProgressBar
    private lateinit var filePhoto: File
    private val FILE_NAME = "photo.jpg"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        RemoveBg.init("PPSVDBtAoLpBzk4UvZU8j2F6")

    }

    @OnClick(R.id.b_choose_image, R.id.i_choose_image)
    fun onChooseImageClicked() {
        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
        alertDialogBuilder.setMessage(R.string.chooseImage)

            .setPositiveButton(R.string.gallery) { dialog, id ->
                gallery = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
                        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISSION_CODE)
                    } else{
                        chooseImageGallery();

                    }
                }else{
                    chooseImageGallery();

                }

            }.setNegativeButton(R.string.camera){dialog, id ->
                gallery = false
                val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                filePhoto = getPhotoFile(FILE_NAME)


                val providerFile =FileProvider.getUriForFile(this,"com.example.androidcamera.fileprovider", filePhoto)
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
                if (takePhotoIntent.resolveActivity(this.packageManager) != null){
                    startActivityForResult(takePhotoIntent, REQUEST_CODE)
                }else {
                    Toast.makeText(this,"Camera could not open", Toast.LENGTH_SHORT).show()
                }
            }
        val alert: android.app.AlertDialog = alertDialogBuilder.create()
        alert.show()



    }



    @OnClick(R.id.iv_input)
    fun onInputClicked() {
        if (inputImage != null) {
            viewImage(inputImage!!)
        } else {
            toast(R.string.error_no_image_selected)
        }
    }



    private fun viewImage(inputImage: File) {

        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", inputImage)

        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(this)
        }
    }

   // @OnClick(R.id.b_process)
   public fun onProcess(fileImage: File) {
        if (inputImage != null) {

            info("Image is ${inputImage!!.path}")
            pbProgress.visibility = View.VISIBLE
                        tvProgress.visibility = View.VISIBLE

                        tvProgress.setText(R.string.status_uploading)
                        pbProgress.progress = 0

                        RemoveBg.from(fileImage, object : RemoveBg.RemoveBgCallback {

                            override fun onProcessing() {
                                runOnUiThread {
                                    tvProgress.setText(R.string.status_processing)
                                }
                            }

                            override fun onUploadProgress(progress: Float) {
                                runOnUiThread {
                                    tvProgress.text = "Uploading ${progress.toInt()}%"
                                    pbProgress.progress = progress.toInt()
                                }
                            }

                            override fun onError(errors: List<ErrorResponse.Error>) {
                                runOnUiThread {
                                    val errorBuilder = StringBuilder()
                                    errors.forEach {
                                        errorBuilder.append("${it.title} : ${it.detail} : ${it.code}\n")
                                    }

                                    showErrorAlert(errorBuilder.toString())
                                    tvProgress.text = errorBuilder.toString()
                                    pbProgress.visibility = View.INVISIBLE
                                }
                            }

                            override fun onSuccess(bitmap: Bitmap) {
                                info("background removed from bg , and output is $bitmap")
                                runOnUiThread {
                                    ivInput.visibility = View.VISIBLE
                                    ivInput.setImageBitmap(bitmap)

                                    tvProgress.visibility = View.INVISIBLE
                                    pbProgress.visibility = View.INVISIBLE

                                }
                            }

                        })


        } else {
            toast(R.string.error_no_image_selected)
        }
    }
    private fun getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor: Cursor? = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }
    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CHOOSE)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    chooseImageGallery()
                }else{
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(!gallery){
            val takenPhoto = BitmapFactory.decodeFile(filePhoto.absolutePath)

            bProcess.isVisible = true
            ivInput.setImageBitmap(takenPhoto)
            ivInput.isVisible = true
            onProcess(filePhoto)
        }

        else{
            uri = data?.data
            inputImage = File(data?.data!!.path)
            bProcess.isVisible = true
            ivInput.setImageURI(data?.data)
            ivInput.isVisible = true
            onProcess(File(getRealPathFromURI(uri!!)))
        }

    }

    companion object {
        private val IMAGE_CHOOSE = 1000;
        private val PERMISSION_CODE = 1001;
        private val REQUEST_CODE = 13
    }
    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }
    /**
     * To show an alert message with title 'Error'
     */
    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_error)
            .setMessage(message)
            .create()
            .show()
    }






    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
