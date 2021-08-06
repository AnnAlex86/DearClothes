package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.drjacky.imagepicker.ImagePicker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.hbb20.CountryPickerView
import com.hbb20.countrypicker.models.CPCountry
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException


class SignUpActivity : AppCompatActivity() {
    var isAvatar = false
    var isCity = false
    private var city:String?=null
    @BindView(R.id.countryPicker)
    lateinit var countryPicker: CountryPickerView

    @BindView(R.id.error_username)
    lateinit var errorUsername: TextView

    @BindView(R.id.error_email)
    lateinit var errorEmail: TextView

    @BindView(R.id.error_password)
    lateinit var errorPassword: TextView

    @BindView(R.id.error_photo)
    lateinit var errorPhoto: TextView

    @BindView(R.id.error_bio)
    lateinit var errorBio: TextView

    @BindView(R.id.avatarButton)
    lateinit var avatarButton: ImageButton

    @BindView(R.id.username_enter)
    lateinit var enterUsername: EditText

    @BindView(R.id.email_enter)
    lateinit var enterEmail: EditText

    @BindView(R.id.enter_password)
    lateinit var enterPassword: EditText

    @BindView(R.id.enter_bio)
    lateinit var enterBio: EditText

    @BindView(R.id.location)
    lateinit var locationButton: Button

    @BindView(R.id.error_city)
    lateinit var errorCity: TextView
    var avatar: Bitmap ?= null
    var imageUrl: String? = null
    var uri: Uri? = null
    var user: FirebaseUser ?= null
    var auth: FirebaseAuth ?= null
   /* @BindView(R.id.spinner_region)
    lateinit var spinnerRegion: Spinner*/
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        countryPicker = findViewById(R.id.countryPicker)
        ButterKnife.bind(this)
        setupCountryPickerView()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
     //   FirebaseApp.initializeApp( this)


    }

    @OnClick(R.id.location)
    fun onLocation() {

      getLastKnownLocation()
    }


    fun getLastKnownLocation() {


        if (checkPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                  //  getAddress(applicationContext, location!!.latitude, location.longitude)
                    val cst = location?.let {
                        CityAsyncTask(
                            this,
                            it.latitude, it.longitude
                        )
                    }
                    cst!!.execute()

                    var lo: String? = null
                    try {
                        lo = cst.get().toString()
                        locationButton.setText(lo)
                        city = lo
                        isCity = true
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }

                    }
                }



    @SuppressLint("WrongConstant")
    private val profileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                 uri = it.data?.data!!
                Glide.with(applicationContext).load(uri).apply(RequestOptions().circleCrop()).into(avatarButton)
                isAvatar = true
            }

            else {
                toast((R.string.errorDownloadPhoto))
            }
        }
    fun uploadAvatar(){

        val bitmap = avatarButton.drawable.toBitmap()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data: ByteArray = baos.toByteArray()
        val fileName = UUID.randomUUID().toString() +".jpg"
        //  val database = FirebaseDatabase.getInstance()
        val storage = Firebase.storage("gs://dear-clothes.appspot.com")

        val refStorage = storage.reference.child("avatar/$fileName")

        refStorage.putBytes(data)
            .addOnSuccessListener(
                OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                        imageUrl = it.toString()
                        addUserDatabase()
                    }
                })

            ?.addOnFailureListener(OnFailureListener { e ->
                print(e.message)
            })
    }


    @OnClick(R.id.avatarButton)
    fun onChooseImageClicked() {
        ImagePicker.with(this)
            // .cropOval()
            .maxResultSize(1080, 1080)
            //.maxResultSize(512, 512, true)
            .createIntentFromDialog { profileLauncher.launch(it) }

    }
    @OnClick(R.id.button_signup)
    fun signUp() {
        if (checkFill()) createRegistration()

    }

     private fun setupCountryPickerView() {
         countryPicker!!.cpViewHelper.onCountryChangedListener = { selectedCountry: CPCountry? ->
           //  errorUsername.text = selectedCountry!!.name
            // spinnerRegion.visibility = View.VISIBLE
         }
     }

    fun checkFill(): Boolean{
        var isFill = true
        if(enterEmail.text.toString().equals("")) {errorEmail.setText(R.string.errorEmail)
            isFill = false}
        else errorEmail.setText("")
        if(enterUsername.text.toString().equals("")) {errorUsername.setText(R.string.errorUsername)
            isFill = false}
        else errorUsername.setText("")
        if(enterPassword.text.toString().equals("")) {errorPassword.setText(R.string.errorPassword)
            isFill = false}
        else errorPassword.setText("")
        if(enterBio.text.toString().equals("")) {errorBio.setText(R.string.errorBio)
            isFill = false}
        else errorBio.setText("")
        if(isAvatar) errorPhoto.setText("")
        else {errorPhoto.setText(R.string.errorPhoto)
            isFill = false}
        if(isCity) errorCity.setText("")
        else {errorCity.setText(R.string.errorCity)
            isFill = false}
        return isFill
    }

    /*fun setUpSpinner(){
        val spinner: Spinner = findViewById(R.id.spinner_region)
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.regionsRu,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
    }
*/
    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    val PERMISSION_ID = 42
    private fun checkPermission(vararg perm:String) : Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this,it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if(perm.toList().any {
                    ActivityCompat.
                    shouldShowRequestPermissionRationale(this, it)}
            ) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Permission")
                    .setMessage("Permission needed!")
                    .setPositiveButton("OK", {id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, PERMISSION_ID)
                    })
                    .setNegativeButton("No", {id, v -> })
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this, perm, PERMISSION_ID)
            }
            return false
        }
        return true
    }
    fun createRegistration(){
       auth = FirebaseAuth.getInstance();
        auth!!.createUserWithEmailAndPassword(enterEmail.text.toString(), enterPassword.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")

                    // user = auth!!.currentUser

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(enterUsername.text.toString())
                        .build()
                   auth!!.signInWithEmailAndPassword(enterEmail.text.toString(), enterPassword.text.toString())
                    checkUser()
                    verifyEmail()
                    user!!.updateProfile(profileUpdates)



                //    uploadAvatar()
                 //   addUserDatabase()
                   // updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    //updateUI(null)
                }
            }
    }

    fun checkUser(){
        if (auth?.currentUser != null) {
            auth!!.currentUser!!.reload()
        } else {
            auth?.signInAnonymously()
                ?.addOnCompleteListener(this,
                    OnCompleteListener<AuthResult?> { task ->
                        Log.d("FirebaseAuth", "signInAnonymously:onComplete:" + task.isSuccessful)

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Log.w("FirebaseAuth", "signInAnonymously", task.exception)
                            Toast.makeText(
                                this, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // ...
                    })
        }
    }

    private fun verifyEmail() {
        user = auth!!.currentUser;

        user!!.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this,
                        "Verification email sent to " + user!!.getEmail(),
                        Toast.LENGTH_SHORT).show()
                   // if(auth!!.uid!=null)
                    uploadAvatar()
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    Toast.makeText(this,
                        "Failed to send verification email.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun addUserDatabase(){
        val db = Firebase.firestore
        //uploadAvatar()
       val user =
         User(enterUsername.text.toString(), enterEmail.text.toString(),
               enterBio.text.toString(), locationButton.text.toString(), imageUrl!!
           )
        db.collection("users").document(enterEmail.text.toString())
            .set(user)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

}

    class CityAsyncTask     // TODO Auto-generated constructor stub
    (var act: Activity, var latitude: Double, var longitude: Double) :
    AsyncTask<String?, String?, String>() {
    override fun doInBackground(vararg p0: String?): String {
        var result = ""

        try {
            val geocoder = Geocoder(act, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.size > 0) {
                val address =
                    addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                val city = addresses[0].locality
                result = city
                val state = addresses[0].adminArea
                val country = addresses[0].countryName
                val postalCode = addresses[0].postalCode
                val knownName = addresses[0].featureName // Only if available else return NULL
                Log.d(TAG, "getAddress:  address$address")
                Log.d(TAG, "getAddress:  city$city")
                Log.d(TAG, "getAddress:  state$state")
                Log.d(TAG, "getAddress:  postalCode$postalCode")
                Log.d(TAG, "getAddress:  knownName$knownName")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    override fun onPostExecute(result: String) {
        // TODO Auto-generated method stub
        super.onPostExecute(result)
    }

    }}