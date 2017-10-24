package edu.csulb.memoriesapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by Danie on 10/16/2017.
 */

public class CreateAccountActivity extends Activity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private EditText userEmail;
    private EditText userPassword;
    private EditText userPassword2;
    private Button createAccountButton;
    private Pattern pattern;
    private Matcher matcher;

    private final String TAG = "CreateAccountActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        mAuth = FirebaseAuth.getInstance();
        userEmail = (EditText) this.findViewById(R.id.user_email_create);
        userPassword = (EditText) this.findViewById(R.id.user_password_create);
        userPassword2 = (EditText) this.findViewById(R.id.user_password_create2);
        createAccountButton = (Button) this.findViewById(R.id.create_account_button_2);
        createAccountButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String userEmailCreate = userEmail.getText().toString();
        String userPass = userPassword.getText().toString();
        String userPass2 = userPassword2.getText().toString();

        if(userEmailCreate.isEmpty()) {
            printToast("Email line is empty.");
        }
        else if(userPass.isEmpty() || userPass2.isEmpty()) {
            printToast("Password line is empty.");
        }
        else if (!passwordGuidelineCheck(userPass)) {
            printToast("Passwords does not meet guidelines.");
        }
        else if(!userPass.equals(userPass2)) {
            printToast("Passwords do not match.");
        }else {
            mAuth.createUserWithEmailAndPassword(userEmailCreate, userPass).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        //TODO: need to send email or phone verification
                        //TODO: Beef up security for this part
                        Intent intent = new Intent(CreateAccountActivity.this, TrendingActivity.class);
                        startActivity(intent);
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidCredentialsException malformedEmailException) {
                            printToast("Email format is not valid.");
                        } catch (FirebaseAuthUserCollisionException duplicateEmail) {
                            printToast("Email already exists.");
                        } catch (Exception exception) {
                            Log.w(TAG, "Failure to create user account", task.getException());
                        }
                    }
                }
            });
        }
    }

    private boolean passwordGuidelineCheck(String password) {

        pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{7,}$");
        matcher = pattern.matcher(password);
        if(matcher.matches()){
            return true;
        }
        return false;
    }

    private void printToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
