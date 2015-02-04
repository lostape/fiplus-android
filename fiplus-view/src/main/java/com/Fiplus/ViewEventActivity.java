package com.Fiplus;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wordnik.client.api.ActsApi;
import com.wordnik.client.api.UsersApi;
import com.wordnik.client.model.Activity;
import com.wordnik.client.model.Attendee;
import com.wordnik.client.model.Location;
import com.wordnik.client.model.Time;
import com.wordnik.client.model.UserProfile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import utils.IAppConstants;

public class ViewEventActivity extends FragmentActivity {

    static final String DATEFORMAT = "MMM-dd-yyyy HH:mm a";

    protected TextView mEventName;
    protected TextView mEventDesc;
    protected Button mJoinEventBtn;
    protected Button mCancelBtn;
    protected RadioGroup mLocationList;
    protected RadioGroup mTimeList;
    protected LinearLayout mAttendeesList;
    protected TextView mAttendeesLabel;

    protected List<Time> mSuggestedTimes = new ArrayList<Time>();
    protected List<Location> mSuggestedLocs = new ArrayList<Location>();
    protected List<String> mAttendees = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        Bundle b = getIntent().getExtras();
        final String mEventID = b.getString("eventID");

        mEventName = (TextView) findViewById(R.id.view_event_name);
        mEventDesc = (TextView) findViewById(R.id.view_event_description);
        mLocationList = (RadioGroup) findViewById(R.id.view_event_loc_radiogroup);
        mTimeList = (RadioGroup) findViewById(R.id.view_event_time_radiogroup);
        mAttendeesList = (LinearLayout)findViewById(R.id.view_event_attendees_list);
        mAttendeesLabel = (TextView) findViewById(R.id.view_event_attendees_label);

        GetEventTask getEventTask = new GetEventTask(mEventID);
        getEventTask.execute();

        mJoinEventBtn = (Button) findViewById(R.id.view_event_join_btn);
        mJoinEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JoinEventTask joinEventTask = new JoinEventTask(mEventID);
                joinEventTask.execute();
            }
        });

        //TODO: (Jobelle) Join Event - Change button label based on user (i.e. isCreator or isJoiner)
        mCancelBtn = (Button) findViewById(R.id.view_event_not_interested_btn);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_in_from_left, R.anim.activity_out_to_right);
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_in_from_left, R.anim.activity_out_to_right);
    }

    class GetEventTask extends AsyncTask<Void, Void, String> {

        ProgressDialog progressDialog;
        Activity response;
        String sEventID, sEventDetails;
        Attendee attendees;

        public GetEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog= ProgressDialog.show(ViewEventActivity.this, getString(R.string.view_event_progress_bar_title) + "...", getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params) {

            ActsApi getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                response = getEventApi.getActivity(sEventID);
                sEventDetails = response.toString();
                attendees = getEventApi.getAttendees(sEventID, 5.0);
            } catch (Exception e) {
                sEventDetails = e.getMessage();
            }

            UsersApi usersApi = new UsersApi();
            usersApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            usersApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);
            UserProfile sUserProfile;

            for(int i=0; i < attendees.getJoiners().size(); i++)
            {
                try {
                    //TODO: (Jobelle) View Event - Get Joiners Profile
                    //sUserProfile = usersApi.getUserProfile(attendees.getJoiners().get(i));
                    //mAttendees.add(sUserProfile.getUsername());
                    mAttendees.add(attendees.getJoiners().get(i));
                } catch (Exception e) {
                    sEventDetails = e.getMessage();
                }
            }

            return sEventDetails;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            mEventName.setText(response.getName());
            mEventDesc.setText(response.getDescription());
            mSuggestedTimes = response.getSuggested_times();
            mSuggestedLocs = response.getSuggested_locations();
            mAttendeesLabel.setText(getString(R.string.view_event_attendees_label) + " (max of " + response.getMax_attendees().intValue() + ")");
            addAttendees();
            addLocation();
            addTime();

            progressDialog.dismiss();
        }

        private void addAttendees()
        {
            //TODO: (Jobelle) View Event - Attendees Profile Pic
            for(int i=0; i < mAttendees.size(); i++) {
                View joiner = getLayoutInflater().inflate(R.layout.joiners_layout, mAttendeesList, false);
                LinearLayout joinersList = (LinearLayout) joiner.findViewById(R.id.joiner_layout);
                TextView joinerName = (TextView) joinersList.findViewById(R.id.joiner_name);
                joinerName.setText(mAttendees.get(i));
                mAttendeesList.addView(joinersList);
            }
        }

        private void addLocation()
        {
            Address addr;
            List<Address> addressList;

            for (int row = 0; row < 1; row++) {
                for (int i = 0; i < mSuggestedLocs.size(); i++)
                {
                    RadioButton addrRadioList = new RadioButton(getBaseContext());
                    addrRadioList.setTextColor(Color.BLACK);
                    addrRadioList.setId((row * 2) + i);
                    try {
                        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.CANADA);
                        addressList = geocoder.getFromLocation(mSuggestedLocs.get(i).getLatitude(),
                                mSuggestedLocs.get(i).getLongitude(), 1);
                        if (addressList != null && addressList.size() > 0) {
                            addr = addressList.get(0);

                            String addressText = String.format(
                                    "%s, %s, %s %s",
                                    // If there's a street address, add it
                                    addr.getMaxAddressLineIndex() > 0 ?
                                            addr.getAddressLine(0) : "",
                                    // Locality is usually a city
                                    addr.getLocality() != null ? addr.getLocality() : "",
                                    // The country of the address
                                    addr.getCountryName(),
                                    // If there's a postal code, add it
                                    addr.getPostalCode() != null ? addr.getPostalCode() : "");

                            addrRadioList.setText(addressText);

                            mLocationList.addView(addrRadioList);
                        }
                    } catch (IOException e) {
                        Log.e("View Event", e.getMessage());
                    }
                }
            }

            if(mSuggestedLocs.size() > 0)
            {
                mLocationList.check(0); //select the first item on the list
            }
        }

        private void addTime()
        {
            for (int row = 0; row < 1; row++) {

                for (int i = 0; i < mSuggestedTimes.size(); i++) {
                    RadioButton rdbtn = new RadioButton(getBaseContext());
                    rdbtn.setTextColor(Color.BLACK);
                    rdbtn.setId((row * 2) + i);
                    rdbtn.setText(convertToTimeToString(mSuggestedTimes.get(i)));
                    mTimeList.addView(rdbtn);
                }
            }

            if(mSuggestedTimes.size() > 0)
            {
                mTimeList.check(0); //select the first item on the list
            }
        }

        private String convertToTimeToString(Time time)
        {
            long startDate = time.getStart().longValue();
            long endDate = time.getEnd().longValue();
            Date d1 = new Date(startDate);
            Date d2 = new Date(endDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
            return "Start: " + dateFormat.format(d1) + "\nEnd  : " + dateFormat.format(d2);
        }
    }

    //TODO: (Jobelle) Join Event - Voting for a suggestion
    class JoinEventTask extends AsyncTask<Void, Void, String>
    {
        String sEventID, response;
        ProgressDialog progressDialog;

        public JoinEventTask (String s)
        {
            sEventID = s;
        }

        @Override
        protected void onPreExecute()
        {
            progressDialog= ProgressDialog.show(ViewEventActivity.this, getString(R.string.join_event_progress_bar_title) + "...", getString(R.string.progress_dialog_text), true);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            ActsApi getEventApi = new ActsApi();
            getEventApi.addHeader("X-DreamFactory-Application-Name", IAppConstants.APP_NAME);
            getEventApi.setBasePath(IAppConstants.DSP_URL + IAppConstants.DSP_URL_SUFIX);

            try {
                getEventApi.joinActivity(sEventID);
            } catch (Exception e) {
                response = e.getMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String message) {
            progressDialog.dismiss();
            message = "Joined Event";
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}