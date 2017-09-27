package kr.ac.kpu.wheeling.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.app.AppConfig;
import kr.ac.kpu.wheeling.app.AppController;
import kr.ac.kpu.wheeling.blackbox.CameraActivity;
import kr.ac.kpu.wheeling.helper.SQLiteHandler;
import kr.ac.kpu.wheeling.helper.SessionManager;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = SignupActivity.class.getSimpleName();
    private Button btnRegister;
    private Button btnLinkToLogin;
    private EditText inputName;
    private EditText inputEmail;
    private EditText inputPassword;
    private EditText inputPassword2;
    private EditText inputNickname;
    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        inputName = (EditText) findViewById(R.id.name);
        inputNickname = (EditText) findViewById(R.id.nickname);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        inputPassword2 = (EditText) findViewById(R.id.password2);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(SignupActivity.this,
                    CameraActivity.class);
            startActivity(intent);
            finish();
        }

        // Register Button Click event
        btnRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                String name = inputName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                String nickname = inputNickname.getText().toString().trim();

                Log.d(TAG, "Signup");

                if (!validate()) {
                    onSignupFailed();
                    return;
                } else {
                    registerUser(email, name, nickname, password);
                }
            }
        });



        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        btnRegister.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = inputName.getText().toString();
        String nickname = inputNickname.getText().toString();
        //String address = _addressText.getText().toString();
        String email = inputEmail.getText().toString();
        //String mobile = _mobileText.getText().toString();
        String password = inputPassword.getText().toString();
        String reEnterPassword = inputPassword2.getText().toString();

        if (name.isEmpty() || name.length() < 2) {
            inputName.setError("이름은 두 글자 이상입니다.");
            valid = false;
        } else {
            inputName.setError(null);
        }

        if (nickname.isEmpty() || name.length() < 2) {
            inputName.setError("두 글자 이상의 닉네임을 입력하여 주십시오.");
            valid = false;
        } else {
            inputName.setError(null);
        }


        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("유효한 이메일 주소를 입력하여 주십시오.");
            valid = false;
        } else {
            inputEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 8) {
            inputPassword.setError("8자리 이상의 패스워드를 입력하여 주십시오.");
            valid = false;
        } else {
            inputPassword.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 8 || !(reEnterPassword.equals(password))) {
            inputPassword2.setError("패스워드가 일치하지 않습니다.");
            valid = false;
        } else {
            inputPassword2.setError(null);
        }

        return valid;
    }

    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     * */
    private void registerUser(final String email,final String name, final String nickname,
                              final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_register";

        pDialog.setMessage("Registering ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        // User successfully stored in MySQL
                        // Now store the user in sqlite
                        //String uid = jObj.getString("uid");

                        JSONObject user = jObj.getJSONObject("user");
                        String name = user.getString("username");
                        String email = user.getString("useremail");
                        String nickname = user.getString("usernick");
                        String regdate = user
                                .getString("regdate");

                        // Inserting row in users table
                        db.addUser(email, name, nickname, regdate);

                        Toast.makeText(getApplicationContext(), "User successfully registered. Try login now!", Toast.LENGTH_LONG).show();

                        // Launch login activity
                        Intent intent = new Intent(
                                SignupActivity.this,
                                LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<>();
                params.put("username", name);
                params.put("usernick", nickname);
                params.put("useremail", email);
                params.put("userpw", password);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
