package be.ap.edu.mapsaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class login : Activity() {
    private lateinit var dbRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val nameField = findViewById<TextView>(R.id.naam_txtview)


        //login_button
        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener{
            val name = nameField?.text.toString()
            //ADD TO FIREBASE //Specify instance or else error
            dbRef = FirebaseDatabase.getInstance("https://carapp-2fa29-default-rtdb.europe-west1.firebasedatabase.app").getReference("person")
            val person= Person(name, name)
            dbRef.child(name).setValue(person)
                .addOnCompleteListener{
                    Toast.makeText(this,"Login", Toast.LENGTH_LONG).show()
                }.addOnFailureListener { err ->
                    Toast.makeText(this,"Error ${err.message}", Toast.LENGTH_LONG).show()
                }
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("person", person)
            startActivity(intent)
        }


    }
}