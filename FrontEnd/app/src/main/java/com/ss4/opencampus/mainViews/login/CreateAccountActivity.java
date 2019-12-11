package com.ss4.opencampus.mainViews.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.ss4.opencampus.R;
import com.ss4.opencampus.mainViews.DashboardActivity;
import com.ss4.opencampus.mainViews.NetworkingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Axel Zumwalt
 *
 * Class that provides functionality for the Create Account Activity.
 *
 * Will check for valid inputs in the EditText fields and then make a POST request to the backend.
 * On successful post, adds a user to the database and logs that user into the app.
 *
 */
public class CreateAccountActivity extends AppCompatActivity {

    private EditText firstName;

    private EditText lastName;

    private EditText userName;

    private EditText email;

    private EditText password;

    private TextView userNameError;

    private TextView emailError;

    private TextView passwordError;

    private TextView emptyError;

    private Context context;

    /**
     * On create method for the CreateAccountActivity. Initilizes instance variables
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_register);

        context = this;

        firstName = (EditText)findViewById(R.id.editText_FirstName);
        lastName = (EditText)findViewById(R.id.editText_LastName);
        userName = (EditText)findViewById(R.id.editText_UserName);
        email = (EditText)findViewById(R.id.editText_Email);
        password = (EditText)findViewById(R.id.editText_Password);

        userNameError = (TextView)findViewById(R.id.textView_userName_error);
        emailError = (TextView)findViewById(R.id.textView_email_error);
        passwordError = (TextView)findViewById(R.id.textView_password_error);
        emptyError = (TextView)findViewById(R.id.textView_empty_error);

        userNameError.setVisibility(View.INVISIBLE);
        emailError.setVisibility(View.INVISIBLE);
        passwordError.setVisibility(View.INVISIBLE);
        emptyError.setVisibility(View.INVISIBLE);
    }

    /**
     * Routine ran when the create account button is pressed. If all fields are valid, add a new student to he database
     *
     * @param view
     */
    public void createAccount(View view) {

        if (validateFields()) {
            JSONObject newStudent = new JSONObject();
            try {
                newStudent.put("firstName", firstName.getText());
                newStudent.put("lastName", lastName.getText());
                newStudent.put("userName", userName.getText());
                newStudent.put("email", email.getText());
                newStudent.put("password", Crypto.encryptAndEncode(password.getText().toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = "http://coms-309-ss-4.misc.iastate.edu:8080/students/add";

            Response.Listener<JSONObject> listenerResponse = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.get("response").equals(true)) {
                            viewDashboardActivity();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            Response.ErrorListener listenerError = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            };

            NetworkingUtils.sendPostObjectRequest(context, url, newStudent, listenerResponse, listenerError);
        }
    }

    /**
     * Checks if the data entered into the datafields is valid.
     * Does checks for data lengths, email validity, and duplicate username or email.
     *
     * @return True if all fields are valid, False otherwise.
     */
    private boolean validateFields() {
        userNameError.setVisibility(View.INVISIBLE);
        emailError.setVisibility(View.INVISIBLE);
        passwordError.setVisibility(View.INVISIBLE);
        emptyError.setVisibility(View.INVISIBLE);

        /* Show emptyError message if any of the editText boxes are empty */
        if (firstName.getText().toString().equals("") || lastName.getText().toString().equals("") || userName.getText().toString().equals("") || email.getText().toString().equals("") || password.getText().toString().equals("")) {
            emptyError.setVisibility(View.VISIBLE);
            return false;
        }
        else {
            /* Shows respective error messages for invalid email and password too short */
            if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
                emailError.setText(R.string.txt_view_invalid_email_error);
                emailError.setVisibility(View.VISIBLE);
            }
            if (password.getText().length() < 8) {
                passwordError.setVisibility(View.VISIBLE);
            }

            String url = "http://coms-309-ss-4.misc.iastate.edu:8080/students/search/all";

            Response.Listener<JSONArray> listenerResponse = new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        boolean dupUsername = false;
                        boolean dupEmail = false;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject student = response.getJSONObject(i);
                            if (student.getString("userName").equals(userName.getText().toString())) {
                                userNameError.setVisibility(View.VISIBLE);
                                dupUsername = true;
                            }
                            if (student.getString("email").equals(email.getText().toString())) {
                                emailError.setText(R.string.txt_view_duplicate_email_error);
                                emailError.setVisibility(View.VISIBLE);
                                dupEmail = true;
                            }
                            if (dupUsername && dupEmail) {
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            Response.ErrorListener listenerError = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            };

            NetworkingUtils.sendGetArrayRequest(context, url, listenerResponse, listenerError);

            /* If there is any error, at least of of the error txtView boxes will be visible */
            if (userNameError.getVisibility() == View.VISIBLE || emailError.getVisibility() == View.VISIBLE || passwordError.getVisibility() == View.VISIBLE) {
                return false;
            }
            else {
                return true;
            }
        }
    }

    /**
     * Method that logs the user into the app if the POST request was successful.
     * Gets the id of the user with a GET request and adds it to Shared Preferences.
     */
    private void viewDashboardActivity()
    {
        String url = String.format("http://coms-309-ss-4.misc.iastate.edu:8080/students/search/email?param1=%1$s", email.getText().toString());

        Response.Listener<JSONArray> listenerResponse = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject student = response.getJSONObject(0);
                    LoginPreferenceUtils.LoginUserId(student.getInt("id"), context);
                    Intent intent = new Intent(context, DashboardActivity.class);
                    startActivity(intent);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener listenerError = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        };

        /* Search for a user with the given email */
        NetworkingUtils.sendGetArrayRequest(context, url, listenerResponse, listenerError);
    }
}


