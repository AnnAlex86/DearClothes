package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.firebase.auth.FirebaseAuth
import java.lang.Exception

class SignInActivity: AppCompatActivity() {
    @BindView(R.id.error_email)
    lateinit var errorEmail: TextView

    @BindView(R.id.error_password)
    lateinit var errorPassword: TextView

    @BindView(R.id.error_signin)
    lateinit var errorSignIn: TextView

    @BindView(R.id.email_enter)
    lateinit var enterEmail: EditText

    @BindView(R.id.enter_password)
    lateinit var enterPassword: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.button_signup)
    fun signUp() {

        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }
    @OnClick(R.id.button_signin)
    fun signIn(){
        var auth = FirebaseAuth.getInstance();
        if (checkFill())
          auth.signInWithEmailAndPassword(enterEmail.text.toString(), enterPassword.text.toString())
              .addOnCompleteListener(this) { task ->
                  if (task.isSuccessful) {
                      val intent = Intent(this, MainActivity::class.java)
                      startActivity(intent)

                  }
                  else {
                      try {
                          throw task.exception!!
                      } // if user enters wrong email.

                      catch (e: Exception) {
                          Log.d(ContentValues.TAG, "onComplete: " + e.message)
                          errorSignIn.setText(e.message)
                         // progressBar.visibility = View.GONE
                          //  Toast.makeText(this, e.message!!, Toast.LENGTH_SHORT).show()
                      }

                  }                  }
    }
    fun checkFill(): Boolean{
        var isFill = true
        if(enterEmail.text.toString().equals("")) {errorEmail.setText(R.string.errorEmail)
            isFill = false}
        else errorEmail.setText("")

        if(enterPassword.text.toString().equals("")) {errorPassword.setText(R.string.errorPassword)
            isFill = false}
        else errorPassword.setText("")

        return isFill
    }

}