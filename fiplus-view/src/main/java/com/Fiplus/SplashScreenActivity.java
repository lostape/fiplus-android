package com.Fiplus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.wordnik.client.api.UsersApi;
import com.wordnik.client.model.Credentials;

import utils.IAppConstants;
import utils.PrefUtil;

/**
 * Created by jsfirme on 15-01-04.
 */
public class SplashScreenActivity extends BaseFragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        new StartLogin().execute();
    }

    private class StartLogin extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            boolean validSession = false;
            //String oldSessionKey = PrefUtil.getString(getApplicationContext(), IAppConstants.SESSION_ID, null);
            String userID = PrefUtil.getString(getApplicationContext(), IAppConstants.EMAIL, null);
            String userPass = PrefUtil.getString(getApplicationContext(), IAppConstants.PWD, null);
            String dspUrl = PrefUtil.getString(getApplicationContext(), IAppConstants.DSP_URL, null);

           // if(oldSessionKey == null &&  userID == null && userPass == null && dspUrl == null){
            if(userID == null && userPass == null && dspUrl == null){
                // show splash for 2 secs and go to login
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{ // previously logged in, check if still valid
                try{
                    UsersApi userApi = new UsersApi();
                    userApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
                    Credentials login = new Credentials();
                    login.setEmail(userID);
                    login.setPassword(userPass);
                    userApi.setBasePath(dspUrl + IAppConstants.DSP_URL_SUFIX);
                    userApi.login(login);
                    validSession = true;
                }catch(Exception e){
                    PrefUtil.putString(getApplicationContext(), IAppConstants.EMAIL, "");
                    PrefUtil.putString(getApplicationContext(), IAppConstants.PWD, "");
                    PrefUtil.putString(getApplicationContext(), IAppConstants.USER_ID, "");
                }
            }
            return validSession;
        }
        @Override
        protected void onPostExecute(Boolean isValidSession) {
            if(isValidSession){
                Intent in= new Intent(SplashScreenActivity.this, MainScreenActivity.class);
                startActivity(in);
            }
            else {
                Intent in=new Intent(SplashScreenActivity.this, LoginActivity.class);
                startActivity(in);
            }
            finish();
        }
    }
}
